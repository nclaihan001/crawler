FROM maven:3-eclipse-temurin-17-focal as builder
WORKDIR /code
COPY pom.xml pom.xml
COPY src src
RUN mvn clean package
FROM mcr.microsoft.com/playwright:jammy
WORKDIR /app
RUN apt update && apt install -y openjdk-17-jdk
COPY --from=builder /code/target/*.jar app.jar
EXPOSE 8080
ENV LANG C.UTF-8
ENTRYPOINT java -jar app.jar


