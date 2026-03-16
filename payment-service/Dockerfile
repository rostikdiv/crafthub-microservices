# --------- STAGE 1: Build ---------
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

WORKDIR /app

# Copy parent pom and pom files of all modules
COPY pom.xml .
COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY product-service/pom.xml ./product-service/
COPY order-service/pom.xml ./order-service/
COPY service-discovery/pom.xml ./service-discovery/
COPY notification-service/pom.xml ./notification-service/
COPY cart-service/pom.xml ./cart-service/
# 👇 Add new services so Maven sees the full structure
COPY payment-service/pom.xml ./payment-service/
COPY delivery-service/pom.xml ./delivery-service/

# Download dependencies (caching)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Copy source code for this specific service
COPY payment-service/src ./payment-service/src

# Build only this module (-pl payment-service)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl payment-service -am

# --------- STAGE 2: Run ---------
FROM eclipse-temurin:17-jre-focal

WORKDIR /app

# Copy built JAR from the target folder of the corresponding module
COPY --from=builder /app/payment-service/target/payment-service*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]