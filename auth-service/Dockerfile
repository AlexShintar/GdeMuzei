FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Копируем ВЕСЬ проект
COPY pom.xml .
COPY museum-contracts ./museum-contracts
COPY auth-service ./auth-service
COPY museum-api-service ./museum-api-service
COPY admin-backend-service ./admin-backend-service
COPY api-gateway ./api-gateway
COPY telegram-bot ./telegram-bot

# Собираем только auth-service
RUN mvn clean package -pl auth-service -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/auth-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
