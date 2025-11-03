# --------- ЕТАП 1: ЗБІРКА (Build) ---------
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder
WORKDIR /app

# 1. Копіюємо ВСІ pom.xml файли
COPY pom.xml .
COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY product-service/pom.xml ./product-service/
COPY order-service/pom.xml ./order-service/
COPY service-discovery/pom.xml ./service-discovery/
COPY notification-service/pom.xml ./notification-service/
COPY cart-service/pom.xml ./cart-service/

# 2. Завантажуємо ВСІ залежності з кешуванням
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 3. Копіюємо вихідний код ТІЛЬКИ для поточного модуля
COPY cart-service/src ./cart-service/src

# 4. Збираємо ТІЛЬКИ наш модуль з кешуванням
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl cart-service -am

# --------- ЕТАП 2: ЗАПУСК (Run) ---------
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# 5. Копіюємо .jar (використовуємо ...-*.jar)
COPY --from=builder /app/cart-service/target/cart-service-*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]