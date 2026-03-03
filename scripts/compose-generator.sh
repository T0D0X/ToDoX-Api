#!/bin/bash

generate_compose() {
    local environment=$1

    if [ "$environment" != "test" ]; then
        echo "❌ ERROR: Only 'test' environment is supported for now"
        exit 1
    fi

    echo "🔧 Generating TEST docker-compose..."

    # Проверяем что директория с миграциями существует
    if [ ! -d "$PROJECT_ROOT/postgres/migrations" ]; then
        echo "❌ ERROR: Migrations directory not found: $PROJECT_ROOT/postgres/migrations"
        exit 1
    fi

    echo "📁 Using migrations from: $PROJECT_ROOT/postgres/migrations"

    generate_test_compose
    validate_compose_syntax
}

generate_test_compose() {
    cat > "$PROJECT_ROOT/docker-compose.yml" << EOF
version: '3.8'

services:
  postgres-test:
    image: ${CONFIG[postgres_version]}
    container_name: todo-postgres-test
    environment:
      POSTGRES_DB: ${CONFIG[test_db]}
      POSTGRES_USER: ${CONFIG[test_user]}
      POSTGRES_PASSWORD: ${CONFIG[test_password]}
    ports:
      - "${CONFIG[test_port]}:5432"
    volumes:
      - $PROJECT_ROOT/postgres/migrations:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${CONFIG[test_user]} -d ${CONFIG[test_db]}"]
      interval: 5s
      timeout: 3s
      retries: 5
    tmpfs:
      - /var/lib/postgresql/data
EOF

    echo "✅ Docker Compose file generated for TEST environment"
}

validate_compose_syntax() {
    echo "🔍 Validating docker-compose syntax..."

    if command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    if ! $COMPOSE_CMD -f "$PROJECT_ROOT/docker-compose.yml" config > /dev/null; then
        echo "❌ ERROR: Invalid docker-compose.yml syntax"
        exit 1
    fi

    echo "✅ Docker Compose syntax is valid"
}

start_services() {
    local environment=$1

    echo "🚀 Starting TEST services..."

    if command -v docker-compose >/dev/null 2>&1; then
            COMPOSE_CMD="docker-compose"
        else
            COMPOSE_CMD="docker compose"
        fi

    $COMPOSE_CMD up -d

    echo "⏳ Waiting for services to be healthy..."
    sleep 5

    # Проверяем доступность базы данных
    if check_db_health; then
        echo "✅ PostgreSQL TEST is running at: localhost:${CONFIG[test_port]}"
        echo "📊 Database: ${CONFIG[test_db]}"
        echo "👤 User: ${CONFIG[test_user]}"
    else
        echo "❌ PostgreSQL health check failed"
        echo "💡 Check logs with: $COMPOSE_CMD logs postgres-test"
        exit 1
    fi
}

check_db_health() {
    if docker exec todo-postgres-test pg_isready -U "${CONFIG[test_user]}" -d "${CONFIG[test_db]}" > /dev/null 2>&1; then
        return 0
    fi
    return 1
}