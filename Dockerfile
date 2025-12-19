# Этап 1: Сборка (Builder)
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

ENV DB_USER=test_user
ENV DB_PASSWORD=test_password
ENV DB_HOST=localhost
ENV DB_NAME=todo_test
ENV DB_PORT=8080

# Установка sbt
RUN apt-get update && \
    apt-get install -y curl gnupg2 && \
    curl -L "https://github.com/sbt/sbt/releases/download/v1.10.0/sbt-1.10.0.tgz" | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/bin/sbt

COPY postgres/ /app/postgres/
COPY build.sbt /app/
COPY project /app/project
COPY src /app/src
COPY postgres/migrations /app/migrations/
COPY . /app
RUN sbt clean compile stage

# Этап 2: Запуск (Runtime)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Установка postgresql-client
RUN apt-get update && \
    apt-get install -y postgresql-client && \
    rm -rf /var/lib/apt/lists/*

# Копирование готового приложения
COPY --from=builder /app/target/universal/stage /app

# Копирование скриптов
COPY scripts/run-migration.sh /app/
RUN chmod +x /app/run-migration.sh

# Порт по умолчанию
ENV PORT=9090

# Точка входа
ENTRYPOINT ["/app/run-migration.sh"]
CMD ["./bin/todox-api", "-Dhttp.port=${PORT}", "-Dhttp.address=0.0.0.0"]
