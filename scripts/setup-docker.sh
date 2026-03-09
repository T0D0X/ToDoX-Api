#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ENVIRONMENT=${1:-test}

echo "🚀 Starting Docker setup for: $ENVIRONMENT"

# Очистка
bash "$SCRIPT_DIR/cleanup.sh"

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  COMPOSE_CMD="docker compose"
fi

$COMPOSE_CMD up -d
sleep 10

bash "$SCRIPT_DIR/run-migration.sh"

echo "✅ Docker setup completed successfully!"