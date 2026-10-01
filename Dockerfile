# Test runner image: JDK, Maven and every dependency, cached in a layer so code changes rebuild fast.
# Browsers run in the Selenium Grid container (see docker-compose.yml), not here.
# Keep the Java version in step with <java.version> in pom.xml (ConsistencyTest checks).
FROM maven:3-eclipse-temurin-26

WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:resolve dependency:resolve-plugins

COPY . .
RUN mvn -B -q -DskipTests compile test-compile

CMD ["mvn", "-B", "verify", "-DskipUnitTests=true"]
