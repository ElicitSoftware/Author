echo "Set java to 25"
jenv local graalvm64-25.0.4
export JAVA_HOME="$(jenv javahome)"

# Survey, Admin and Author must agree on which languages Elicit carries, and each must declare
# the bundles it actually ships. This is the authoritative run of that check: Author is the
# module most likely to break the invariant -- a language Author has and Survey lacks lets an
# author publish survey content no respondent could ever read (Author#40) -- and now that Author
# lives outside the umbrella's clone script, this is the only build that sees all three modules.
# It needs the surrounding umbrella checkout for ../Survey and ../Admin; SKIP_LANGUAGE_CHECK=1
# builds without it, for a tree that has no umbrella around it.
if [ "${SKIP_LANGUAGE_CHECK:-0}" != 1 ]; then
    if ! ./checkLanguages.sh; then
        echo "Aborting: the applications disagree on their languages (see above)." >&2
        exit 1
    fi
fi

echo "build Author"

# The author's manual (UC-039) is typeset before the package step so the PDF is on the
# classpath when Quarkus builds the image, and is stamped with the version and build date
# this image carries (UC-039 BR-003, NFR-016). SKIP_MANUAL=1 builds without it; the
# application then simply does not offer the link (UC-039 A1).
VERSION="$(sed -n '1,20p' pom.xml | grep -m1 -o '<version>[^<]*</version>' | sed 's/<[^>]*>//g')"
BUILD_DATE="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
./docs/manual/build-manual.sh "${VERSION}" "${BUILD_DATE}"

# Vaadin reuses the cached production bundle (src/main/bundles/prod.bundle, gitignored) whenever
# the set of frontend modules is unchanged -- even when a new route needs its own lazy chunk.
# A stale bundle then ships routes whose dialogs render without their components. Drop the
# cache so every image gets a freshly built bundle.
rm -f src/main/bundles/prod.bundle

./mvnw clean package -Dquarkus.profile=docker -Dquarkus.container-image.build=true
