#!/bin/bash

# Выход при любой ошибке
set -e

ENVIRONMENT=${1:-test}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
CONFIG_FILE="$PROJECT_ROOT/src/main/resources/application.conf"

echo "🧹 Cleaning up old containers..."
docker-compose down 2>/dev/null || true

echo "📁 Using config: $CONFIG_FILE"

# Проверяем что конфиг файл существует
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ ERROR: Config file not found: $CONFIG_FILE"
    echo "💡 Create config file or run with default values"
    exit 1
fi

# Функция для парсинга с проверкой
get_val() {
    local key=$1
    local value
    value=$(grep -E "^\s*${key}\s*=" "$CONFIG_FILE" 2>/dev/null | \
            head -1 | \
            cut -d'=' -f2- | \
            sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | \
            sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

    if [ -z "$value" ]; then
        echo "⚠️  WARNING: Key '$key' not found in config, using default value" >&2
    fi
    echo "$value"
}

# Парсим значения с дефолтами
prod_port=$(get_val "prod.port")
prod_port=${prod_port:-5432}
prod_db=$(get_val "prod.db")
prod_db=${prod_db:-todo_app}
prod_user=$(get_val "prod.user")
prod_user=${prod_user:-app_user}
prod_password=$(get_val "prod.password")
prod_password=${prod_password:-app_password}

test_port=$(get_val "test.port")
test_port=${test_port:-5433}
test_db=$(get_val "test.db")
test_db=${test_db:-todo_test}
test_user=$(get_val "test.user")
test_user=${test_user:-test_user}
test_password=$(get_val "test.password")
test_password=${test_password:-test_password}

postgres_version=$(get_val "docker.postgres-version")
postgres_version=${postgres_version:-15}
postgres_image="postgres:${postgres_version}"

# Проверяем обязательные значения
if [ -z "$prod_db" ] || [ -z "$test_db" ]; then
    echo "❌ ERROR: Database names are required"
    exit 1
fi

echo "🔧 Config values:"
echo "   PROD: port=$prod_port, db=$prod_db, user=$prod_user"
echo "   TEST: port=$test_port, db=$test_db, user=$test_user"
echo "   Image: $postgres_image"
echo "   Image: $postgres_image"

# Проверяем что порты не конфликтуют
if [ "$prod_port" = "$test_port" ]; then
    echo "❌ ERROR: PROD and TEST ports cannot be the same: $prod_port"
    exit 1
fi

# Проверяем что порты доступны
check_port() {
    local port=$1
    local service=$2
    if netstat -tuln 2>/dev/null | grep -q ":$port "; then
        echo "❌ ERROR: Port $port is already in use ($service)"
        exit 1
    fi
}

if [ "$ENVIRONMENT" = "prod" ]; then
    check_port "$prod_port" "PROD"
fi

if [ "$ENVIRONMENT" = "test" ]; then
    check_port "$test_port" "TEST"
fi

# Генерация docker-compose
case $ENVIRONMENT in
  "prod")
    echo "🔧 Generating PRODUCTION environment..."
    cat > "$PROJECT_ROOT/docker-compose.yml" << EOF
version: '3.8'

services:
  postgres-prod:
    image: $postgres_image
    container_name: todo-postgres-prod
    environment:
      POSTGRES_DB: $prod_db
      POSTGRES_USER: $prod_user
      POSTGRES_PASSWORD: $prod_password
    ports:
      - "$prod_port:5432"
    volumes:
      - postgres_prod_data:/var/lib/postgresql/data
      - $PROJECT_ROOT/postgres/migrations:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $prod_user -d $prod_db"]
      interval: 10s
      timeout: 5s
      retries: 3

volumes:
  postgres_prod_data:
EOF
    ;;

  "test")
    echo "🧪 Generating TEST environment..."
    cat > "$PROJECT_ROOT/docker-compose.yml" << EOF
version: '3.8'

services:
  postgres-test:
    image: $postgres_image
    container_name: todo-postgres-test
    environment:
      POSTGRES_DB: $test_db
      POSTGRES_USER: $test_user
      POSTGRES_PASSWORD: $test_password
    ports:
      - "$test_port:5432"
    volumes:
      - $PROJECT_ROOT/postgres/migrations:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $test_user -d $test_db"]
      interval: 5s
      timeout: 3s
      retries: 5
EOF
    ;;

  *)
    echo "❌ ERROR: Unknown environment: $ENVIRONMENT"
    echo "💡 Usage: $0 {prod|test|all}"
    exit 1
    ;;
esac

# Проверяем что файл создался
if [ ! -f "$PROJECT_ROOT/docker-compose.yml" ]; then
    echo "❌ ERROR: Failed to create docker-compose.yml"
    exit 1
fi

echo "✅ Docker Compose generated for: $ENVIRONMENT"
echo "📁 Location: $PROJECT_ROOT/docker-compose.yml"

# Проверяем синтаксис docker-compose файла
echo "🔍 Validating docker-compose syntax..."
if ! docker-compose -f "$PROJECT_ROOT/docker-compose.yml" config > /dev/null; then
    echo "❌ ERROR: Invalid docker-compose.yml syntax"
    exit 1
fi

echo "🎉 All checks passed! You can now run: docker-compose up -d"