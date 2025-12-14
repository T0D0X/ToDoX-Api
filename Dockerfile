# Этап 1: Сборщик
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 1. Копируем только файлы, необходимые для загрузки зависимостей
COPY project/ ./project/
COPY build.sbt ./

# 2. (Опционально) Этим шагом можно ускорить сборку, предварительно загрузив зависимости.
# Если сборка долгая, раскомментируйте следующую строку:
# RUN sbt update

# 3. Копируем весь исходный код и собираем "fat jar"
COPY src/ ./src/
COPY TodoApp.scala ./
RUN sbt assembly

# Этап 2: Финальный образ
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем собранный JAR из этапа сборщика
COPY --from=builder /app/target/scala-3.3.7/*-assembly-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]