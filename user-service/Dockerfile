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

# Download dependencies (only for this module to save time)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy the source code for the user-service
COPY user-service/src ./user-service/src

# Build the package, again using a mount cache for the Maven repository
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl user-service -am

# --------- STAGE 2: RUN ---------
# Use a minimal JRE image for the execution stage
FROM eclipse-temurin:17-jre-focal

# Set the working directory
WORKDIR /app

# Copy the compiled .jar file from the builder stage
COPY --from=builder /app/user-service/target/user-service*.jar app.jar

# Expose the port the application listens on
EXPOSE 8081

# Command to execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]