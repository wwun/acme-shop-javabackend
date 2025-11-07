FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=acme-build /app/auth-service/target/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "app.jar" ]