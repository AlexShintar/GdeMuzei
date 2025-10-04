FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY museum-contracts ./museum-contracts
COPY auth-service ./auth-service
COPY museum-api-service ./museum-api-service
COPY admin-backend-service ./admin-backend-service
COPY api-gateway ./api-gateway
COPY telegram-bot ./telegram-bot

RUN mvn clean package -pl museum-api-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/museum-api-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
