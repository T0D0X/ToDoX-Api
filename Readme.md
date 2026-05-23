[![Scala CI](https://github.com/T0D0X/ToDoX-Api/actions/workflows/scala.yml/badge.svg)](https://github.com/T0D0X/ToDoX-Api/actions/workflows/scala.yml)
[![Coverage Status](https://coveralls.io/repos/github/T0D0X/ToDoX-Api/badge.svg?branch=main&kill_cache=1)](https://coveralls.io/github/T0D0X/ToDoX-Api)
# ToDoX-Api

Простое и эффективное REST API для управления задачами, построенное на стеке **Scala 3 + ZIO 2 + ZIO HTTP + Doobie**.

## 🚀 Особенности

- **Функциональное программирование** - полностью иммутабельное, типобезопасное
- **Асинхронность** - построено на ZIO для высокой производительности
- **Безопасность** - контейнеризация через Docker, валидация данных
- **Простота развертывания** - готовые Docker конфигурации
- **Полное CRUD** - создание, чтение, обновление, удаление задач
- **Поиск и фильтрация** - по названию, статусу, приоритету

## 🛠 Технологический стек

- **Scala 3** - язык программирования
- **ZIO 2** - функциональная эффект-система
- **ZIO HTTP** - веб-фреймворк
- **Doobie** - функциональный доступ к данным
- **PostgreSQL** - база данных
- **Docker** - контейнеризация

## 📦 Быстрый старт

### Требования

- Docker & Docker Compose
- Java 11+
- sbt (только для разработки)

## 📊 Мониторинг и метрики

Приложение предоставляет исчерпывающие метрики в формате **Prometheus** на эндпоинте `/metrics`.  
Они охватывают все ключевые аспекты работы сервиса

### Запуск мониторинга
Метрики собираются и визуализируются с помощью связки **Prometheus + Grafana**, уже включённой в `docker-compose.yml`.  
Запустите окружение командой `./scripts/setup-docker.sh` (или `docker compose up -d prometheus grafana`).

- **Prometheus** доступен на [http://localhost:9090](http://localhost:9090)  
  Конфигурация скрейпинга (`prometheus.yml`) задаёт интервал опроса – по умолчанию **10 секунд**.
- **Grafana** доступна на [http://localhost:3000](http://localhost:3000)  
  Логин / пароль по умолчанию: `admin` / `admin` (можно изменить в `docker-compose`).  
  Источник данных Prometheus добавляется автоматически через provisioning.

### Готовые дашборды

Проект уже содержит преднастроенные дашборды Grafana, которые автоматически подгружаются при запуске (provisioning).  
Вам не нужно создавать их вручную – после старта контейнеров они сразу доступны.

- **JVM Micrometer** – дашборд по JVM н
- **HTTP‑запросы** – дашборд по HTTP Request, который включает панели:
    - Rate запросов (`http_requests_total`)
    - Перцентили длительности ответов (p50, p90, p99) на основе гистограммы `http_request_duration_milliseconds`
    - Распределение по методам и путям

Если вы хотите изменить или дополнить дашборды, отредактируйте файлы в папке `grafana/provisioning/dashboards/` – изменения применятся при перезапуске Grafana.


### Запуск контейнера
Здесь находится окружение для интеграционных тестов и для локального запуска приложения

```bash

chmod +x ./scripts/setup-docker.sh
./scripts/setup-docker.sh
```


## 🗄 Модель данных

```scala
case class TodoItem(
  id: UUID,
  userId: UUID,
  title: String,
  description: Option[String],
  isComplete: Boolean,
  priority: Priority, // low/medium/high
  createdAt: LocalDateTime,
  tags: List[String]
)

case class UserData(
    userId: UUID,
    login: String,
    email: String,
    phone: String,
    passwordHash: String,
)
```


