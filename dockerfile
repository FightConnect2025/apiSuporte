# --------- STAGE 1: Build ---------
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# --------- STAGE 2: Runtime ---------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/apiSuporte-0.0.1-SNAPSHOT.jar app.jar

ARG PROFILE=dev
ARG PORT=8086

EXPOSE ${PORT}

ENV SPRING_PROFILES_ACTIVE=${PROFILE}

ENTRYPOINT ["java", "-jar", "app.jar"]
