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

[Swagger ToDoX-Api](https://todox-api-c8r2.onrender.com/docs/#/)

### Запуск контейнера
Здесь находится окружение для интеграционных тестов в дальнейшем сделаем для локального запуска приложения

```bash

chmod +x ./scripts/setup-docker.sh
./scripts/setup-docker.sh
```

## 📚 API Endpoints

### Задачи (todo_items)

| Метод | Endpoint | Описание |
|-------|----------|----------|
⚠️ в процессе придумывания


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
```


