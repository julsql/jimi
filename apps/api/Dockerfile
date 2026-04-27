# Multi-stage build: compile the jar with Maven, then run it on a slim JRE.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependencies in their own layer so source changes don't invalidate them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# -------------------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/target/jimi-api.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
