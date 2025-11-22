#!/bin/bash

echo "🧹 Cleaning up old containers and volumes..."

# Останавливаем и удаляем контейнеры
docker-compose down --remove-orphans --volumes 2>/dev/null || true

# Удаляем конкретные контейнеры если остались
docker rm -f todo-postgres-test 2>/dev/null || true

# Очищаем неиспользуемые volumes (но не данные продакшена)
docker volume prune -f 2>/dev/null || true

echo "✅ Cleanup completed"