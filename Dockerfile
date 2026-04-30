FROM gradle:8.14.4-jdk21-alpine AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S boki && adduser -S boki -G boki
COPY --from=build /app/build/libs/*.jar app.jar

USER boki
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
