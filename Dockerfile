# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon -x test

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем jar
COPY --from=build /app/build/libs/*.jar app.jar

# Переменные окружения
ENV PORT=8080
ENV DATABASE_URL=jdbc:postgresql://db:5432/ktor_db
ENV DATABASE_USER=postgres
ENV DATABASE_PASSWORD=postgres
ENV JWT_SECRET=production-secret-change-me

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
