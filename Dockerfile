# Этап 1: Сборка (Builder)
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Установка sbt (оптимизированная версия)
RUN apt-get update && \
    apt-get install -y curl gnupg2 ca-certificates && \
    curl -fsSL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --dearmor > /usr/share/keyrings/sbt.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/sbt.gpg] https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list && \
    apt-get update && \
    apt-get install -y sbt

# Оптимизация памяти для SBT
ENV SBT_OPTS="-Xmx2G -Xss2M -XX:MaxMetaspaceSize=1G"
ENV SBT_IVY_HOME=/root/.ivy2
ENV SBT_HOME=/usr/share/sbt

# Сначала копируем только файлы для зависимостей (кешируемый слой)
COPY build.sbt /app/
COPY project/ /app/project/

# Скачиваем зависимости
RUN sbt update

# Затем копируем всё остальное
COPY . /app

# Копируем миграции в отдельную папку
COPY postgres/migrations /app/migrations/

# Сборка
RUN sbt clean compile stage

# Этап 2: Запуск (Runtime)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Установка postgresql-client для миграций
RUN apt-get update && \
    apt-get install -y postgresql-client && \
    rm -rf /var/lib/apt/lists/*

# Копирование готового приложения из builder этапа
COPY --from=builder /app/target/universal/stage /app

# Копирование миграций из builder этапа
COPY --from=builder /app/migrations /app/migrations

# Копирование скриптов
COPY scripts/run-migration.sh /app/
RUN chmod +x /app/run-migration.sh /app/bin/todox-api

# Порт по умолчанию
ENV PORT=9090

# Переменные для БД (можно переопределить при запуске)
ENV DB_USER=postgres
ENV DB_PASSWORD=postgres
ENV DB_HOST=localhost
ENV DB_NAME=todo_db
ENV DB_PORT=8080

# Точка входа
ENTRYPOINT ["/app/run-migration.sh"]
CMD ["./bin/todox-api", "-Dhttp.port=${PORT}", "-Dhttp.address=0.0.0.0"]