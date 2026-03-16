# --------- STAGE 1: Build ---------
# Using the official Maven image with Java 17
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy ONLY pom.xml files first (parent then service)
# This leverages Docker caching: if dependencies haven't changed,
# Docker won't redownload them.
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

# Use Docker BuildKit cache for the .m2 folder
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy the source code
# Ensure full path is specified
COPY order-service/src ./order-service/src

# Build while mounting the .m2 cache to minimize downloads
# Diagnostic information
RUN mvn -version
RUN mvn dependency:tree -pl order-service

# Build the package (skipping tests for build speed)
RUN mvn clean package -DskipTests -pl order-service -am

# --------- STAGE 2: Run ---------
# Use a minimal image with only the Java Runtime
FROM eclipse-temurin:17-jre-focal

# Set the working directory
WORKDIR /app

# Copy the .jar file from STAGE 1 (builder)
COPY --from=builder /app/order-service/target/order-service*.jar app.jar

# Expose the port the application listens on
EXPOSE 8083

# Command to execute when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]