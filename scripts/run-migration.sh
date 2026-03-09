#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MIGRATIONS_DIR="$PROJECT_ROOT/postgres/migrations"
CONTAINER_NAME="todo-postgres-test"  # имя контейнера из docker-compose.yml

# Определяем способ подключения
if command -v psql >/dev/null 2>&1; then
    USE_DOCKER=false
    echo "🔧 Режим: локальный psql"
else
    USE_DOCKER=true
    echo "🔧 Режим: docker exec (psql внутри контейнера)"
fi

wait_for_db() {
    local max_attempts=10
    local attempt=1

    echo "Ожидание доступности базы данных..."

    for attempt in $(seq 1 $max_attempts); do
        echo "Попытка $attempt из $max_attempts..."

        if [ "$USE_DOCKER" = true ]; then
            if docker exec -e PGPASSWORD="$DB_PASSWORD" "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; then
                echo "✓ База данных доступна"
                return 0
            fi
        else
            if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; then
                echo "✓ База данных доступна"
                return 0
            fi
        fi
        sleep 2
    done

    echo "✗ База данных недоступна после $max_attempts попыток"
    return 1
}

run_migrations() {
    echo "Проверка наличия миграций..."

    if [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ] || [ -z "$DB_HOST" ] || [ -z "$DB_NAME" ]; then
        echo "⚠ Пропускаем миграции: не все переменные БД установлены"
        return 0
    fi

    # Проверяем наличие папки с миграциями
    if [ ! -d "$MIGRATIONS_DIR" ]; then
        echo "⚠ Папка $MIGRATIONS_DIR не найдена. Пропускаем."
        return 0
    fi

    # Ищем SQL файлы
    local migration_files=$(find "$MIGRATIONS_DIR" -name "*.sql" | sort)
    if [ -z "$migration_files" ]; then
        echo "⚠ Файлы миграций не найдены. Пропускаем."
        return 0
    fi

    echo "Найдено $(echo "$migration_files" | wc -l)"

    for migration_file in $migration_files; do
        local filename=$(basename "$migration_file")
        echo "Выполняем: $filename"

        if [ "$USE_DOCKER" = true ]; then
            # Внутри контейнера файлы доступны по пути /docker-entrypoint-initdb.d/
            local container_path="/docker-entrypoint-initdb.d/$filename"
            if docker exec -e PGPASSWORD="$DB_PASSWORD" "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -f "$container_path"; then
                echo "✓ Успешно: $filename"
            else
                echo "✗ Ошибка в миграции: $filename"
                return 1
            fi
        else
            if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$migration_file"; then
                echo "✓ Успешно: $filename"
            else
                echo "✗ Ошибка в миграции: $filename"
                return 1
            fi
        fi
    done

    echo "✅ Все миграции выполнены"
    return 0
}

main() {
    echo "=== Запуск контейнера ==="

    # Пропускаем миграции если нужно
    if [ "$SKIP_MIGRATIONS" = "true" ] || [ "$SKIP_MIGRATIONS" = "1" ]; then
        echo "Пропускаем миграции (SKIP_MIGRATIONS=true)"
    else
        # Ждём БД
        if ! wait_for_db; then
            echo "Не удалось подключиться к БД. Запускаем приложение без миграций..."
        else
            # Выполняем миграции
            if ! run_migrations; then
                echo "ВНИМАНИЕ: Ошибка при выполнении миграций, но продолжаем запуск..."
            fi
        fi
    fi

    # Запускаем основное приложение
    echo "Запуск основного приложения..."
    exec "$@"
}

main "$@"