FROM maven:3.9.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S artiv && adduser -S artiv -G artiv \
    && mkdir -p /app/uploads \
    && chown -R artiv:artiv /app

WORKDIR /app
COPY --from=build --chown=artiv:artiv /workspace/target/artiv-0.0.1-SNAPSHOT.jar /app/artiv.jar

USER artiv
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/artiv.jar"]
