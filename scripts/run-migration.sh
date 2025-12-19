#!/bin/bash
set -e

wait_for_db() {
    local max_attempts=30
    local attempt=1

    echo "Ожидание доступности базы данных..."

    for attempt in $(seq 1 $max_attempts); do
        echo "Попытка $attempt из $max_attempts..."

        if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c '\q' 2>/dev/null; then
            echo "✓ База данных доступна"
            return 0
        fi

        sleep 2
    done

    echo "✗ База данных недоступна после $max_attempts попыток"
    return 1
}

run_migrations() {
    echo "Проверка наличия миграций..."

    # Проверяем обязательные переменные
    if [ -z "$DB_USER" ] || [ -z "$DB_PASSWORD" ] || [ -z "$DB_HOST" ] || [ -z "$DB_NAME" ]; then
        echo "⚠ Пропускаем миграции: не все переменные БД установлены"
        return 0
    fi

    # Проверяем наличие папки с миграциями
    if [ ! -d "/app/migrations" ]; then
        echo "⚠ Папка /app/migrations не найдена. Пропускаем."
        return 0
    fi

    # Ищем SQL файлы
    local migration_files=$(find /app/migrations -name "*.sql" | sort)
    if [ -z "$migration_files" ]; then
        echo "⚠ Файлы миграций не найдены. Пропускаем."
        return 0
    fi

    echo "Найдено $(echo "$migration_files" | wc -l) файлов миграции"

    # Просто выполняем все миграции по порядку
    for migration_file in $migration_files; do
        local filename=$(basename "$migration_file")
        echo "Выполняем: $filename"

        # Выполняем SQL файл
        if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$migration_file"; then
            echo "✓ Успешно: $filename"
        else
            echo "✗ Ошибка в миграции: $filename"
            return 1
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