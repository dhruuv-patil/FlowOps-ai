# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system flowops && useradd --system --gid flowops flowops
COPY --from=build /build/target/flowops-api.jar app.jar

EXPOSE 8080
# The readiness probe is the right one for a healthcheck: it returns 200 only
# once the app has finished bootstrapping (DB, Flyway, bean graph). /health is
# liveness, which would let a half-booted container appear "healthy" and start
# accepting traffic before the DB was actually ready.
#
# curl is not in the JRE image; install it for the healthcheck. The image
# stays small (curl is ~200 KB) and the alternative — a sidecar probe or
# shelling out to `java -cp` — is heavier.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

USER flowops
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/health/ready || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
