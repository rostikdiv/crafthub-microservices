# --------- STAGE 1: BUILD ---------
# Use the official Maven image with Java 17 for the build stage
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

# Set the working directory
WORKDIR /app

# 1. Copy the parent pom.xml
COPY pom.xml .

# 2. Copy the pom.xml files for all modules to support incremental caching
COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY product-service/pom.xml ./product-service/
COPY order-service/pom.xml ./order-service/
COPY service-discovery/pom.xml ./service-discovery/
COPY notification-service/pom.xml ./notification-service/
COPY cart-service/pom.xml ./cart-service/
COPY payment-service/pom.xml ./payment-service/
COPY delivery-service/pom.xml ./delivery-service/

# 3. Download dependencies for all modules using cache
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 4. Copy the source code ONLY for the current module
COPY notification-service/src ./notification-service/src

# 5. Build only the notification-service module with caching
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl notification-service -am

# --------- STAGE 2: RUN ---------
# Use a minimal JRE image for the execution stage
FROM eclipse-temurin:17-jre-focal

# Set the working directory
WORKDIR /app

# 6. Copy the compiled .jar file from the builder stage
COPY --from=builder /app/notification-service/target/notification-service-*.jar app.jar

# Expose the service port
EXPOSE 8084

# Command to execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]