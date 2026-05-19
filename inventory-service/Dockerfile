FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
#COPY --from=acme-build /app/inventory-service/target/*.jar app.jar
COPY target/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "app.jar" ]

#acme-build is created by command when the image is generated