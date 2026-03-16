# --------- STAGE 1: Build Phase ---------
# Use Maven and OpenJDK 17 for building the application
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder
WORKDIR /app

# 1. Copy root POM and module POMs to leverage Docker layer caching for dependencies
COPY pom.xml .
COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY product-service/pom.xml ./product-service/
COPY order-service/pom.xml ./order-service/
COPY service-discovery/pom.xml ./service-discovery/
COPY notification-service/pom.xml ./notification-service/
COPY cart-service/pom.xml ./cart-service/
COPY payment-service/pom.xml ./payment-service/
COPY delivery-service/pom.xml ./delivery-service/

# 2. Download all dependencies offline to speed up subsequent builds
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 3. Copy the source code for the cart-service module
COPY cart-service/src ./cart-service/src

# 4. Build the cart-service package, skipping unit tests for faster deployment
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl cart-service -am

# --------- STAGE 2: Runtime Phase ---------
# Use a lightweight JRE image for the final production container
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# 5. Copy the generated JAR file from the builder stage
COPY --from=builder /app/cart-service/target/cart-service-*.jar app.jar

# Expose the default port for the cart-service
EXPOSE 8085

# Execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]