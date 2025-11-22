#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENVIRONMENT=${1:-test}

source "$SCRIPT_DIR/config-loader.sh"
source "$SCRIPT_DIR/compose-generator.sh"

echo "🚀 Starting Docker setup for: $ENVIRONMENT"

# Очистка
bash "$SCRIPT_DIR/cleanup.sh"

#Загрузка конфигурации
load_config

# Проверка конфигурации
validate_config "$ENVIRONMENT"

# Генерация docker-compose
generate_compose "$ENVIRONMENT"

# Запуск сервисов
start_services "$ENVIRONMENT"

echo "✅ Docker setup completed successfully!"