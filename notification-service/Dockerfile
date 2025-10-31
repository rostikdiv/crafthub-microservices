# --------- ЕТАП 1: ЗБІРКА (Build) ---------
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder
WORKDIR /app

# 1. Копіюємо батьківський pom.xml
COPY pom.xml .

# 2. Копіюємо ВСІ pom.xml модулів (включаючи новий)
COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY product-service/pom.xml ./product-service/
COPY order-service/pom.xml ./order-service/
COPY service-discovery/pom.xml ./service-discovery/
COPY notification-service/pom.xml ./notification-service/

# 3. Завантажуємо ВСІ залежності з кешуванням
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 4. Копіюємо вихідний код ТІЛЬКИ для поточного модуля
COPY notification-service/src ./notification-service/src

# 5. Збираємо ТІЛЬКИ наш модуль з кешуванням
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl notification-service -am

# --------- ЕТАП 2: ЗАПУСК (Run) ---------
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# 6. Копіюємо .jar з правильного місця (використовуємо ...-*.jar)
COPY --from=builder /app/notification-service/target/notification-service-*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]