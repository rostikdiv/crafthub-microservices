# --------- STAGE 1: Build ---------
# Using official Maven image with Java 17
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

# Set working directory inside container
WORKDIR /app

# Copy ONLY pom.xml files (parent first, then child modules)
# Docker caching optimization: if dependencies haven't changed,
# Docker will reuse the cached layer.
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

# Use Docker BuildKit cache for .m2 directory
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy remaining source code of service discovery
COPY service-discovery/src ./service-discovery/src

# Build package using .m2 cache
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl service-discovery -am

# --------- STAGE 2: Run ---------
# Minimal runtime image with JRE
FROM eclipse-temurin:17-jre-focal

# Set working directory
WORKDIR /app

# Copy built .jar from builder stage
COPY --from=builder /app/service-discovery/target/service-discovery*.jar app.jar

# Expose container port
EXPOSE 8761

# Container entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]