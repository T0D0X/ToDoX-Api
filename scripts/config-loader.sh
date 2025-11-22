#!/bin/bash

CONFIG_FILE="$PROJECT_ROOT/src/main/resources/application.conf"

declare -A CONFIG

load_config() {
    echo "📁 Loading configuration from: $CONFIG_FILE"

    if [ ! -f "$CONFIG_FILE" ]; then
        echo "❌ ERROR: Config file not found: $CONFIG_FILE"
        exit 1
    fi

    # Загружаем только тестовые настройки
    CONFIG[test_port]=$(get_required_config_value "test.port")
    CONFIG[test_db]=$(get_required_config_value "test.db")
    CONFIG[test_user]=$(get_required_config_value "test.user")
    CONFIG[test_password]=$(get_required_config_value "test.password")

    CONFIG[postgres_version]=$(get_required_config_value "postgres.image")

    echo "✅ Configuration loaded successfully"
}

get_required_config_value() {
    local key=$1
    local value

    value=$(grep -E "^\s*${key}\s*=" "$CONFIG_FILE" 2>/dev/null | \
            head -1 | \
            cut -d'=' -f2- | \
            sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | \
            sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

    if [ -z "$value" ]; then
        echo "❌ ERROR: Required configuration key '$key' not found in $CONFIG_FILE" >&2
        exit 1
    fi

    echo "$value"
}

validate_config() {
    local environment=$1

    echo "🔍 Validating configuration..."

    # Проверяем что порт числовой
    if ! [[ "${CONFIG[test_port]}" =~ ^[0-9]+$ ]] || [ "${CONFIG[test_port]}" -lt 1 ] || [ "${CONFIG[test_port]}" -gt 65535 ]; then
        echo "❌ ERROR: Invalid TEST port: ${CONFIG[test_port]}"
        exit 1
    fi

    # Проверяем существование директории с миграциями
    if [ ! -d "$PROJECT_ROOT/postgres/migrations" ]; then
        echo "❌ ERROR: Migrations directory not found: $PROJECT_ROOT/postgres/migrations"
        exit 1
    fi

    # Проверяем что есть хотя бы одна миграция
    if [ -z "$(find "$PROJECT_ROOT/postgres/migrations" -name "*.sql" -type f)" ]; then
        echo "❌ ERROR: No SQL migration files found in: $PROJECT_ROOT/postgres/migrations"
        exit 1
    fi

    # Проверяем доступность портов
    check_port "${CONFIG[test_port]}" "TEST"

    echo "✅ Configuration validation passed"
}

check_port() {
    local port=$1
    local service=$2

    if command -v nc >/dev/null 2>&1; then
        if nc -z localhost "$port" 2>/dev/null; then
            echo "❌ ERROR: Port $port is already in use ($service)"
            exit 1
        fi
    elif command -v ss >/dev/null 2>&1; then
        if ss -tuln | grep -q ":${port} "; then
            echo "❌ ERROR: Port $port is already in use ($service)"
            exit 1
        fi
    else
        echo "⚠️  WARNING: Skipping port check (nc/ss not available)"
    fi
}