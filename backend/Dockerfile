# syntax=docker/dockerfile:1
# One parameterised build for all five Spring Boot services.
# Build arg SERVICE is the module directory name, e.g. "movie-service".
FROM eclipse-temurin:17-jdk AS build
ARG SERVICE
WORKDIR /build
COPY . .
# sharing=locked serialises the five parallel image builds so they cannot
# race while the Maven Wrapper extracts its distribution into the cache.
RUN --mount=type=cache,id=cinemind-maven-v2,target=/root/.m2,sharing=locked \
    chmod +x mvnw \
 && ./mvnw -B -f backend/${SERVICE}/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre AS runtime
ARG SERVICE
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/backend/${SERVICE}/target/${SERVICE}-1.0.0-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
