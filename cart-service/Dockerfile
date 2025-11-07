FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=acme-build /app/cart-service/target/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "app.jar" ]