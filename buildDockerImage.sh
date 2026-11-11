echo "Set java to 25"
jenv local 25
export JAVA_HOME="$(jenv javahome)"

# Survey, Admin and Author must agree on which languages Elicit carries: the images carry English
# only, the curated ../elicit-i18n mount gives all three the same translated languages, no
# translated bundle is back on a classpath, and Author publishes content in exactly that set.
# Author is the module most likely to break the invariant -- a language Author has and Survey
# lacks lets an author publish survey content no respondent could ever read (Author#40) -- so
# its own build runs the umbrella's check too. It needs the surrounding umbrella checkout for
# ../Survey, ../Admin and ../elicit-i18n; SKIP_LANGUAGE_CHECK=1 builds without it, and the
# umbrella's buildDockerImages.sh sets it because that run has already checked.
if [ "${SKIP_LANGUAGE_CHECK:-0}" != 1 ]; then
    if ! ../checkLanguages.sh; then
        echo "Aborting: the applications disagree on their languages (see above)." >&2
        exit 1
    fi
fi

echo "build Author"

# The author's manual (UC-039) ships inside the image (BR-004), stamped with the version
# and build date of the build that typeset it (BR-003, NFR-016). Typesetting means a TeX Live
# container and the slowest minute of a development build, so this script reuses the PDF a
# previous run left at src/main/resources/manual/ and typesets only when there is none; a
# reused manual then carries that earlier build's stamp, which an unpublished image can live
# with. BUILD_MANUAL=1 typesets a fresh one; SKIP_MANUAL=1 builds without one, and the
# application then simply does not offer the link (UC-039 A1). Published images never take
# the shortcut: the release workflow (.github/workflows/maven.yml) typesets the manual for
# every tagged build, pre-release or final.
MANUAL_PDF="src/main/resources/manual/elicit-author-manual.pdf"
if [ "${BUILD_MANUAL:-0}" != 1 ] && [ "${SKIP_MANUAL:-0}" != 1 ] && [ -s "${MANUAL_PDF}" ]; then
    echo "[manual] reusing ${MANUAL_PDF} from $(date -r "${MANUAL_PDF}" '+%Y-%m-%d %H:%M'); BUILD_MANUAL=1 typesets a fresh one"
else
    VERSION="$(sed -n '1,20p' pom.xml | grep -m1 -o '<version>[^<]*</version>' | sed 's/<[^>]*>//g')"
    BUILD_DATE="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
    ./docs/manual/build-manual.sh "${VERSION}" "${BUILD_DATE}"
fi

# Vaadin reuses the cached production bundle (src/main/bundles/prod.bundle, gitignored) whenever
# the set of frontend modules is unchanged -- even when a new route needs its own lazy chunk.
# A stale bundle then ships routes whose dialogs render without their components. Drop the
# cache so every image gets a freshly built bundle.
rm -f src/main/bundles/prod.bundle

./mvnw clean package -Dquarkus.profile=docker -Dquarkus.container-image.build=true
