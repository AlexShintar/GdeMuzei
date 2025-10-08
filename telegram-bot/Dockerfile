# ---------- deps ----------
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /build
COPY pom.xml .
COPY museum-contracts/pom.xml museum-contracts/pom.xml
COPY telegram-bot/pom.xml telegram-bot/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests dependency:go-offline -pl telegram-bot -am

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY --from=deps /root/.m2 /root/.m2
COPY pom.xml .
COPY museum-contracts museum-contracts
COPY telegram-bot telegram-bot
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests -pl telegram-bot -am package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 appuser
USER appuser
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -Dfile.encoding=UTF-8"
COPY --from=build /build/telegram-bot/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
