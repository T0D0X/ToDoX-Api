# Этап 1: Сборка (Builder)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Установка sbt (замените версию на актуальную)
RUN apt-get update && \
    apt-get install -y curl gnupg2 && \
    curl -L "https://github.com/sbt/sbt/releases/download/v1.10.0/sbt-1.10.0.tgz" | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

# Копирование файлов проекта и сборка
COPY . .
RUN sbt clean compile stage

# Этап 2: Запуск (Runtime)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копирование готового приложения из этапа сборки
COPY --from=builder /app/target/universal/stage /app

# Команда для запуска (зависит от типа вашего проекта)
# Пример для Play Framework или приложения с запуском через скрипт:
CMD ["./bin/todox-api"]