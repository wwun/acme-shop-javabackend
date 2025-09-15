# FROM maven:3.9.6-eclipse-temurin-17 AS build
# WORKDIR /app
# # COPY . .
# COPY pom.xml .
# COPY acme-commons/pom.xml acme-commons/
# COPY acme-commons/src acme-commons/src
# COPY order-service/pom.xml order-service/
# COPY order-service/src order-service/src
# RUN mvn -pl order-service -am clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=acme-build /app/order-service/target/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "app.jar" ]

#acme-build is created by command when the image is generated