echo "Set java to 25"
jenv local 25
echo "build Author"

./mvnw clean package -Dquarkus.profile=docker -Dquarkus.container-image.build=true
