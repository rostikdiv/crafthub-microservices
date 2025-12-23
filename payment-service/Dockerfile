# --------- ЕТАП 1: ЗБІРКА (Build) ---------
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

WORKDIR /app

# Копіюємо батьківський pom та pom-файли всіх модулів
COPY pom.xml .
COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY product-service/pom.xml ./product-service/
COPY order-service/pom.xml ./order-service/
COPY service-discovery/pom.xml ./service-discovery/
COPY notification-service/pom.xml ./notification-service/
COPY cart-service/pom.xml ./cart-service/
# 👇 Додаємо нові сервіси, щоб Maven бачив повну структуру
COPY payment-service/pom.xml ./payment-service/
COPY delivery-service/pom.xml ./delivery-service/

# Завантажуємо залежності (кешування)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# Копіюємо вихідний код саме цього сервісу
COPY payment-service/src ./payment-service/src

# Збираємо тільки цей модуль (-pl payment-service)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl payment-service -am

# --------- ЕТАП 2: ЗАПУСК (Run) ---------
FROM eclipse-temurin:17-jre-focal

WORKDIR /app

# Копіюємо зібраний JAR з папки target відповідного модуля
COPY --from=builder /app/payment-service/target/payment-service*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]