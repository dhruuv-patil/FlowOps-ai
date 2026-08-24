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
USER flowops
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
