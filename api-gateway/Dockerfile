# --------- ЕТАП 1: ЗБІРКА (Build) ---------
# Використовуємо офіційний образ Maven з Java 17
FROM maven:3.9.6-eclipse-temurin-17-focal AS builder

# Встановлюємо робочу директорію всередині контейнера
WORKDIR /app

# Копіюємо ТІЛЬКИ pom.xml файли (спочатку батьківський, потім наш)
# Це трюк кешування Docker. Якщо залежності не змінились,
# Docker не буде завантажувати їх знову.
COPY pom.xml .

COPY api-gateway/pom.xml ./api-gateway/
COPY user-service/pom.xml ./user-service/
COPY product-service/pom.xml ./product-service/
COPY order-service/pom.xml ./order-service/
COPY service-discovery/pom.xml ./service-discovery/
COPY notification-service/pom.xml ./notification-service/
COPY cart-service/pom.xml ./cart-service/

# 3. Завантажуємо ВСІ залежності
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline
# Копіюємо решту коду нашого проекту
COPY api-gateway/src ./api-gateway/src

# Збираємо наш .jar файл
# -DskipTests = не запускати тести під час збірки образу
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl api-gateway -am

# --------- ЕТАП 2: ЗАПУСК (Run) ---------
# Використовуємо МІНІМАЛЬНИЙ образ тільки з Java Runtime
FROM eclipse-temurin:17-jre-focal

# Встановлюємо робочу директорію
WORKDIR /app

# Копіюємо наш .jar файл з ЕТАПУ 1 (builder)
# Зверни увагу на шлях, Maven кладе jar в папку 'target'
COPY --from=builder /app/api-gateway/target/api-gateway*.jar app.jar

# Вказуємо порт, який наш додаток буде слухати всередині контейнера
EXPOSE 8080

# Команда, яка виконається при запуску контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]