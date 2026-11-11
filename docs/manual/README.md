# Author's manuals

Two manuals live here, built by the same script with the same brand package:

- **The author's manual** (UC-039), `Building a Survey`, which ships inside the
  image. Most of this page is about it.
- **The installation manual** (UC-050, UC-051), `Installing Author`, a release
  artifact attached to each tagged release and never packaged (C-027). See
  [The installation manual](#the-installation-manual) below.

## The author's manual

`Building a Survey` — the printable, illustrated manual that ships inside the
application image. It is the printable form of the in-app `How surveys work`
guide (UC-029), with the step-by-step procedure added.

See [`../use_cases/UC-039-consult-the-authors-manual.md`](../use_cases/UC-039-consult-the-authors-manual.md)
for the behavior this implements, and FR-047 / NFR-016 in
[`../requirements.md`](../requirements.md).

## What is here

| Path | Committed? | What it is |
| --- | --- | --- |
| `elicit-author-manual.tex` | yes | the author's manual |
| `elicit-author-installation-manual.tex` | yes | the installation manual |
| `../../../docs/manual/elicit-brand.sty` | in the umbrella | the Elicit default brand as LaTeX — colors, IBM Plex, the page furniture, the call-out boxes and the settings tables (UC-039 BR-005, C-028). Read in place; there is no copy here |
| `images/` | yes | the captured screens, plus `elicit-logo.png` for the title page |
| `capture/` | yes | `capture-screenshots.mjs`, its own `package.json`, and `fixtures/` — the sample survey the figures walk; regenerates `images/` from a running Author |
| `build-manual.sh` | yes | typesets either manual: the author's manual where the application packages it, the installation manual beside its source |
| `elicit-author-installation-manual.pdf` | **no** | generated: the installation manual |
| `build-info.tex` | **no** | generated: the version and build date stamped into this build |
| `build/` | **no** | generated: LaTeX's working directory and the PDF |

## Building it

```bash
docs/manual/build-manual.sh                       # version from pom.xml, date is now
docs/manual/build-manual.sh 3.0.0 "2026-09-24"    # explicit stamp
```

`buildDockerImage.sh` runs it with the project version and the build date
before `mvn package` when no PDF is there yet, and otherwise reuses the one a
previous run left, so that a development build does not wait on TeX Live every
time; `BUILD_MANUAL=1` typesets a fresh one. A reused manual carries the stamp
of the build that typeset it. The release workflow
(`.github/workflows/maven.yml`) typesets it for every tagged build, pre-release
or final, so a published image always carries a manual stamped with its own
version (UC-039 BR-004). The PDF lands at
`src/main/resources/manual/elicit-author-manual.pdf` —
deliberately **not** under `META-INF/resources`, which Quarkus would serve
without a role check. It reaches the browser only through `ManualResource`
(`/api/manual`), which requires `elicit_author` (UC-039 A4).

There is no TeX installation to maintain: the script typesets inside a TeX Live
container (`TEXLIVE_IMAGE`, default `texlive/texlive:latest`), which is where
IBM Plex and the LaTeX packages come from. Docker must be running, and Author
must sit inside the umbrella checkout: the container reads the brand package
from the umbrella's `docs/manual/` (mounted read-only, found through `TEXINPUTS`),
the one copy every Elicit manual is typeset with (C-028).

`SKIP_MANUAL=1` builds the application without the manual. The application then
simply does not offer the link (UC-039 A1) — nothing else changes.

## The version stamp

`build-manual.sh` writes `build-info.tex`, which `elicit-brand.sty` reads. The
version and build date appear on the title page and in the footer of **every**
page (NFR-016). A build that supplies neither leaves both reading `unknown`
rather than omitting them (UC-039 A3, BR-003), so a printed copy is never
silently undated.

## Regenerating the screenshots

The figures are committed. Regenerate them only when the screens they show
change.

```bash
cd …/Elicit && docker compose up -d                  # the umbrella stack
npm --prefix Author/docs/manual/capture install      # playwright
npm --prefix Author/docs/manual/capture run capture
```

The capture tool keeps its own `package.json` so that Playwright stays out of
the Vaadin-managed one at the project root.

The script signs in, walks the sample survey **Family Health History** and
writes `images/NN-*.png` at 2× for print. It needs no greenfield database and
nothing authored by hand: when the deployment does not already hold that survey
it imports `capture/fixtures/family-health-history.elicit` through Import
definition (UC-005) first. The fixture is the structure the manual describes:

- steps `About you` and `Each child`
- sections `You` (questions `First name`, `Has children`, `Child count`) and
  `Child` (questions `Child name`, `Child sex`)
- lists `Yes / No` and `Sex at birth`
- a Show rule `Has children = Yes → Child count`, and a Repeat rule
  `Child count > 0 → section Child`
- an analysis tag `children` on dimension `family`, assigned to `Has children`

Change the fixture and the figures change with it; the element keys are derived
from a fixed namespace, so re-importing it into a fresh Author database always
produces the same survey.

A figure whose screen cannot be found costs that figure and not the run: the
script reports what it missed at the end and exits non-zero, so a changed
selector is visible in CI rather than only in the PDF.

Env: `AUTHOR_URL` (default `http://localhost:8084`), `AUTHOR_USER`,
`AUTHOR_PASSWORD`, `MANUAL_IMAGES`, `MANUAL_SURVEY`,
`MANUAL_SURVEY_DEFINITION`.

After regenerating, check that every `\screenshot{…}` in the `.tex` still
matches what the figure shows — the caption is part of the instruction.

## The installation manual

`Installing Author` takes an operator from no Author to a running, verified one:
the `author` database, the `elicit-author` client and its roles, the preview
Survey, both installation paths, the configuration reference and verification.
See [`../use_cases/UC-050-install-author-from-the-installation-manual.md`](../use_cases/UC-050-install-author-from-the-installation-manual.md)
for what it covers and
[`../use_cases/UC-051-build-the-author-installation-manual.md`](../use_cases/UC-051-build-the-author-installation-manual.md)
for how it is built.

```bash
MANUAL=installation docs/manual/build-manual.sh                    # version from pom.xml
MANUAL=installation docs/manual/build-manual.sh 3.0.0 "2026-09-24"  # explicit stamp
```

The PDF is written beside its source as `elicit-author-installation-manual.pdf`
and is not committed. Before typesetting, the script runs the umbrella's
`docs/manual/check-properties.sh --modules Author` against it: every key Author
reads must be documented in the configuration reference, and every default it
prints must be the value in `application.properties` (NFR-021, UC-051 A3).
`SKIP_PROPERTY_CHECK=1` typesets without the check. A setting that belongs to the
preview Survey rather than to Author is written with `\optlit{}`, which the check
does not count, so Author is not held to Survey's keys.

The release workflow typesets it for every tag and attaches it to that tag's
GitHub release, creating the release when there is none yet (UC-051 BR-004, A6).
