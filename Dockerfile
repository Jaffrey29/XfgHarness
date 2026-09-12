FROM eclipse-temurin:17-jre-alpine AS base
WORKDIR /app

FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY deepseek-harness-java-api/pom.xml deepseek-harness-java-api/
COPY deepseek-harness-java-app/pom.xml deepseek-harness-java-app/
COPY deepseek-harness-java-case/pom.xml deepseek-harness-java-case/
COPY deepseek-harness-java-domain/pom.xml deepseek-harness-java-domain/
COPY deepseek-harness-java-infrastructure/pom.xml deepseek-harness-java-infrastructure/
COPY deepseek-harness-java-trigger/pom.xml deepseek-harness-java-trigger/
COPY deepseek-harness-java-types/pom.xml deepseek-harness-java-types/
COPY plugins/sample-tools-plugin/pom.xml plugins/sample-tools-plugin/
COPY plugins/deepseek-harness-plugin-archetype/pom.xml plugins/deepseek-harness-plugin-archetype/
RUN mvn dependency:go-offline -q
COPY . .
RUN mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /src/deepseek-harness-java-app/target/deepseek-harness-java-app-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=standalone"]
