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

$COMPOSE_CMD up -d postgres-test

echo "⏳ Waiting for PostgreSQL..."
until $COMPOSE_CMD exec -T postgres-test pg_isready -U test_user -d todo_test; do
  sleep 1
done


echo "Start migration"
if ! $COMPOSE_CMD run --rm postgres-migration; then
  echo "Migration failed, exiting"
  exit 1
fi
echo "Finish migration"