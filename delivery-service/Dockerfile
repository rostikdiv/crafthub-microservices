# --------- ЕТАП 1: ЗБІРКА (Build) ---------
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

WORKDIR /app

# Копіюємо всі pom.xml
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

# Завантажуємо залежності
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Копіюємо код
COPY delivery-service/src ./delivery-service/src

# Збираємо модуль
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl delivery-service -am

# --------- ЕТАП 2: ЗАПУСК (Run) ---------
FROM eclipse-temurin:17-jre-focal

WORKDIR /app

COPY --from=builder /app/delivery-service/target/delivery-service*.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]