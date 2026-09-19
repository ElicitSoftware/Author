echo "Set java to 25"
jenv local 25
export JAVA_HOME="$(jenv javahome)"
echo "build Author"

# Vaadin reuses the cached production bundle (src/main/bundles/prod.bundle, gitignored) whenever
# the set of frontend modules is unchanged -- even when a new route needs its own lazy chunk.
# A stale bundle then ships routes whose dialogs render without their components. Drop the
# cache so every image gets a freshly built bundle.
rm -f src/main/bundles/prod.bundle

./mvnw clean package -Dquarkus.profile=docker -Dquarkus.container-image.build=true
