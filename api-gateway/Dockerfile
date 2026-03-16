# --------- STAGE 1: BUILD ---------
# Use the official Maven image with Java 17 for the build stage
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy ONLY pom.xml files first (parent then modules)
# This leverages Docker's layer caching for dependencies.
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

# 3. Download all dependencies using cache
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy the source code for the api-gateway
COPY api-gateway/src ./api-gateway/src

# Build only the api-gateway module, skipping tests and using cache
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl api-gateway -am

# --------- STAGE 2: RUN ---------
# Use a minimal JRE image for the execution stage
FROM eclipse-temurin:17-jre-focal

# Set the working directory
WORKDIR /app

# Copy the compiled .jar file from the builder stage
COPY --from=builder /app/api-gateway/target/api-gateway*.jar app.jar

# Expose the gateway port
EXPOSE 8080

# Command to execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]