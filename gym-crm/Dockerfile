# syntax=docker/dockerfile:1

# ---------- Stage 1: build the executable jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Resolve dependencies first so this layer is cached until pom.xml changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Tests are run separately with `mvn test`; the image build only packages the application
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- Stage 2: minimal runtime image ----------
FROM eclipse-temurin:17-jre

# Run as an unprivileged user
RUN groupadd --system gym && useradd --system --gid gym --no-create-home gym
WORKDIR /app

# Copy the Spring Boot layers from least to most frequently changing for better layer caching
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
RUN mkdir logs && chown gym:gym logs

USER gym
EXPOSE 8080

# No secrets here: the profile, database settings, signing keys and EUREKA_URL are supplied at container startup.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS -o /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
