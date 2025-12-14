FROM eclipse-temurin:21-jre
WORKDIR /app
# Копируем всё содержимое подготовленной стадии
COPY target/universal/stage/ .
# Используем скрипт запуска, который создал native-packager
ENTRYPOINT ["./bin/todox-api"]