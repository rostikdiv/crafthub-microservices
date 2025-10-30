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
# 3. Завантажуємо залежності (тільки для цього модуля)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 4. Копіюємо вихідний код
# ❗️ ОСЬ ТВОЯ ПОМИЛКА: ми маємо вказати повний шлях
COPY user-service/src ./user-service/src

# Збираємо, але знову монтуємо кеш .m2, щоб не качати нічого зайвого
RUN --mount=type=cache,target=/root/.m2 mvn clean package -DskipTests -pl user-service -am

# --------- ЕТАП 2: ЗАПУСК (Run) ---------
# Використовуємо МІНІМАЛЬНИЙ образ тільки з Java Runtime
FROM eclipse-temurin:17-jre-focal

# Встановлюємо робочу директорію
WORKDIR /app

# Копіюємо наш .jar файл з ЕТАПУ 1 (builder)
# Зверни увагу на шлях, Maven кладе jar в папку 'target'
COPY --from=builder /app/user-service/target/user-service*.jar app.jar

# Вказуємо порт, який наш додаток буде слухати всередині контейнера
EXPOSE 8081

# Команда, яка виконається при запуску контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]