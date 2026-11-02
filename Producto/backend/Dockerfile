FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario sin privilegios (no correr como root)
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /app/target/*.jar macrofit-backend.jar
USER app

EXPOSE 8080

# MaxRAMPercentage: evita que la JVM use más memoria de la que da el hosting (planes gratuitos ~512 MB)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "macrofit-backend.jar"]
