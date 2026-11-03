#!/bin/bash
#
# Checks the languages Elicit offers: the Survey, Admin and Author images carry English only, the
# curated translations in ../elicit-i18n/ give all three applications the same set of languages,
# and Author publishes survey content in exactly that set.
#
# The images carry English only (Elicit#62, Author#79). Every other language is a
# translations_<tag>.properties file a site mounts at <i18n.file.system.path>/<app>/; ../elicit-i18n/
# in the umbrella is the curated set ElicitSoftware maintains beside each release, mounted by the
# docker-compose.yml here and by the umbrella's. Nothing at runtime can reconcile any of this: an
# application offers whatever files it finds under its own sub-directory, and Author separately
# declares which languages a survey's content may be published in (author.content.languages). Four
# things go wrong silently and are checked here:
#
#   1. A translated bundle reaches a module's classpath again (src/main/resources/vaadin-i18n or a
#      leftover i18n/translations_*.properties the pom might package). The image would then carry a
#      language the site did not mount, and a mounted file would only overlay it.
#   2. A module still sets i18n.bundled.locales. That property no longer exists; a site that kept it
#      would believe it had narrowed its languages when nothing reads it. i18n.offered.locales is
#      the setting now.
#   3. elicit-i18n/survey, elicit-i18n/admin and elicit-i18n/author hold different languages. A
#      language one application has and another lacks cannot be served, because Survey renders
#      content only in a language its own chrome has (Survey UC-009 BR-009).
#   4. Author's author.content.languages is not that same set. A language Author has but Survey
#      lacks lets an author publish survey content that no respondent could ever read -- which is
#      the failure Author#40 exists to prevent, reintroduced from the other end.
#
# So this is an invariant the build holds rather than a convention. This is Author's copy and it
# is the authoritative one: Author is the module most likely to break the invariant, and it is the
# only place all three modules can be seen at once now that Author is a private repository outside
# the umbrella's clone script. It runs from this repo's root and reads ../Survey, ../Admin and
# ../elicit-i18n, so it needs the surrounding Elicit umbrella checkout (see README.md).
# buildDockerImage.sh runs it before the image is built. Cheap enough to run unconditionally: it
# reads files, starts no JVM.
#
# The umbrella keeps a copy that checks Survey and Admin only and merely warns about Author.
set -uo pipefail
cd "$(dirname "$0")"

MODULES=(Survey Admin Author)
# Where each module's checkout is, relative to this script. Author is this repo; the other two
# are siblings in the umbrella one level up, and so is the curated set.
PATHS=(../Survey ../Admin .)
MOUNT=../elicit-i18n
status=0
# Indexed array, aligned with MODULES: /bin/bash on macOS is 3.2 and has no associative arrays.
mounted=()

# English is not a translated bundle: it is authored in each image at
# src/main/resources/vaadin-i18n/translations.properties, so there is no translations_en.properties
# to find. It is implicit everywhere and added here rather than discovered.
tags_in() {  # directory -> comma-separated sorted tags, en included
    local dir=$1
    local tag f
    local list=en
    for f in "$dir"/translations_*.properties; do
        [ -e "$f" ] || continue
        tag=${f##*/translations_}
        tag=${tag%.properties}
        # Bundle files spell a tag with underscores (translations_es_419.properties); every tag in
        # a property, a database column and the runtime uses hyphens. Normalize or nothing matches.
        list="$list
${tag//_/-}"
    done
    printf '%s\n' "$list" | sort -u | paste -sd, -
}

tags_from_property() {  # module path, property name -> comma-separated sorted tags
    # Separate statements on purpose: `local` expands all its arguments before it assigns any, so
    # `local module=$1 file="$module/..."` reads module while it is still unset -- which set -u
    # turns into an error rather than an empty path.
    local module=$1
    local prop=$2
    local file="$module/src/main/resources/application.properties"
    local value
    [ -f "$file" ] || { echo "MISSING-FILE"; return; }
    value=$(grep -E "^${prop//./\\.}=" "$file" | tail -1 | cut -d= -f2-)
    [ -n "$value" ] || { echo "UNSET"; return; }
    printf '%s\n' "${value//,/$'\n'}" | tr -d ' ' | grep -v '^$' | sort -u | paste -sd, -
}

app_dir() {  # module -> its sub-directory of the mount (i18n.app.name)
    echo "$1" | tr '[:upper:]' '[:lower:]'
}

if [ ! -d "$MOUNT" ]; then
    echo "ERROR: $MOUNT is not here, so the languages cannot be checked. This repo expects to sit" >&2
    echo "       inside an Elicit umbrella checkout (Elicit/Author/), which carries elicit-i18n/." >&2
    exit 1
fi

for i in "${!MODULES[@]}"; do
    m=${MODULES[$i]}
    path=${PATHS[$i]}
    if [ ! -d "$path" ]; then
        # Never skipped: a missing clone is exactly when the invariant would lapse unnoticed.
        echo "ERROR: $m is not at $path, so its languages cannot be checked. This repo expects to" >&2
        echo "       sit inside an Elicit umbrella checkout (Elicit/Author/); run the umbrella's" >&2
        echo "       ./cloneAllProjects.sh there." >&2
        status=1
        continue
    fi

    # 1. The image carries English only.
    for f in "$path"/src/main/resources/vaadin-i18n/translations_*.properties "$path"/i18n/translations_*.properties; do
        [ -e "$f" ] || continue
        echo "ERROR: $f would put a translated bundle in the $m image." >&2
        echo "       The images carry English only; move it to $MOUNT/$(app_dir "$m")/." >&2
        status=1
    done

    # 2. The retired property.
    if [ "$(tags_from_property "$path" i18n.bundled.locales)" != UNSET ]; then
        echo "ERROR: $m still sets i18n.bundled.locales, which nothing reads any more." >&2
        echo "       Use i18n.offered.locales to offer fewer languages than are mounted." >&2
        status=1
    fi

    if [ ! -d "$MOUNT/$(app_dir "$m")" ]; then
        echo "ERROR: $MOUNT/$(app_dir "$m")/ is missing; $m would offer English only." >&2
        status=1
    fi
    mounted[$i]=$(tags_in "$MOUNT/$(app_dir "$m")")
done
[ "$status" -eq 0 ] || exit 1

# 3. The curated set gives all three applications the same languages.
reference=${mounted[0]}
for i in 1 2; do
    if [ "${mounted[$i]}" != "$reference" ]; then
        echo "ERROR: $MOUNT/$(app_dir "${MODULES[$i]}")/ holds [${mounted[$i]}] but" \
             "$MOUNT/$(app_dir "${MODULES[0]}")/ holds [$reference]." >&2
        echo "       A language one application has and another lacks cannot be served: Survey" >&2
        echo "       renders content only in a language its own chrome has." >&2
        status=1
    fi
done

# 4. Author's publishable content languages must be that same set.
content=$(tags_from_property . author.content.languages)
if [ "$content" != "$reference" ]; then
    echo "ERROR: Author's author.content.languages is [$content] but $MOUNT/ gives the applications [$reference]." >&2
    echo "       An author could publish survey content in a language no site can render." >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "Images carry English only; $MOUNT/ gives Survey, Admin and Author [$reference]"
fi
exit $status
