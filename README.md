# Ktor Final API

REST API на Ktor с PostgreSQL, JWT аутентификацией, ролями пользователей и WebSocket чатом.

## Технологии

- **Ktor** 2.3.7 - веб-фреймворк
- **PostgreSQL** - база данных
- **Exposed** - ORM
- **JWT** - аутентификация
- **BCrypt** - хеширование паролей
- **WebSocket** - реалтайм чат
- **Swagger UI** - документация API
- **Docker** - контейнеризация

## Быстрый старт с Docker

```bash
# Запуск всего стека (API + PostgreSQL)
docker-compose up -d

# Проверить логи
docker-compose logs -f app

# Остановить
docker-compose down
```

Сервер: http://localhost:8080

## Запуск локально (без Docker)

### 1. Запустить PostgreSQL

```bash
# Через Docker
docker run -d --name postgres \
  -e POSTGRES_DB=ktor_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2. Запустить приложение

```bash
./gradlew run
```

## Документация API

### Swagger UI
```
http://localhost:8080/swagger
```

### OpenAPI
```
http://localhost:8080/openapi
```

## Роли пользователей

| Роль | Права |
|------|-------|
| USER | CRUD своих задач |
| ADMIN | Управление всеми пользователями и задачами |

### Админ по умолчанию
- Username: `admin`
- Password: `admin123`

## API Endpoints

### Аутентификация

| Метод | URL | Описание |
|-------|-----|----------|
| POST | /api/auth/register | Регистрация |
| POST | /api/auth/login | Вход |

### Задачи (требуют JWT)

| Метод | URL | Описание |
|-------|-----|----------|
| GET | /api/tasks | Все задачи пользователя |
| GET | /api/tasks?completed=true | Фильтр по статусу |
| GET | /api/tasks?search=text | Поиск |
| GET | /api/tasks/{id} | Задача по ID |
| POST | /api/tasks | Создать |
| PUT | /api/tasks/{id} | Обновить |
| DELETE | /api/tasks/{id} | Удалить |

### Админ-панель (только ADMIN)

| Метод | URL | Описание |
|-------|-----|----------|
| GET | /api/admin/users | Все пользователи |
| GET | /api/admin/users/{id} | Пользователь по ID |
| PUT | /api/admin/users/{id}/role | Изменить роль |
| DELETE | /api/admin/users/{id} | Удалить пользователя |
| GET | /api/admin/tasks | Все задачи |
| DELETE | /api/admin/tasks/{id} | Удалить любую задачу |

### WebSocket чат

| URL | Описание |
|-----|----------|
| ws://localhost:8080/ws/chat?token=JWT | Подключение к чату |
| GET /api/chat/online | Онлайн пользователи |

## Примеры запросов (PowerShell)

```powershell
# Вход админом
$login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }

# Регистрация нового пользователя
Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"user1","email":"user1@mail.com","password":"1234"}'

# Создать задачу
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Method POST -ContentType "application/json" -Headers $headers -Body '{"title":"Задача","description":"Описание"}'

# Получить все задачи
Invoke-RestMethod -Uri "http://localhost:8080/api/tasks" -Headers $headers

# Админ: получить всех пользователей
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users" -Headers $headers

# Админ: изменить роль пользователя
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/users/2/role" -Method PUT -ContentType "application/json" -Headers $headers -Body '{"role":"ADMIN"}'
```

## WebSocket чат

### Подключение (JavaScript)
```javascript
const token = "YOUR_JWT_TOKEN";
const ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`);

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log(data.type, data.payload);
};

// Отправить сообщение
ws.send("Привет всем!");
```

### Типы сообщений от сервера
- `history` - история чата при подключении
- `chat` - новое сообщение
- `notification` - уведомление (вход/выход)

## Тестирование

```bash
# Запуск тестов
./gradlew test
```

Тесты используют H2 in-memory базу данных.

## Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| PORT | Порт сервера | 8080 |
| DATABASE_URL | JDBC URL | jdbc:postgresql://localhost:5432/ktor_db |
| DATABASE_USER | Пользователь БД | postgres |
| DATABASE_PASSWORD | Пароль БД | postgres |
| JWT_SECRET | Секрет JWT | ktor-final-secret-key-2024 |

## Структура проекта

```
src/main/kotlin/com/example/
├── Application.kt           # Точка входа
├── database/
│   ├── DatabaseFactory.kt   # Конфигурация PostgreSQL
│   └── Tables.kt            # Таблицы Exposed
├── models/
│   └── Models.kt            # Data классы
├── plugins/
│   ├── Plugins.kt           # Конфигурация плагинов
│   └── Routing.kt           # Маршруты + Swagger
├── repository/
│   ├── UserRepository.kt    # CRUD пользователей
│   ├── TaskRepository.kt    # CRUD задач
│   └── ChatRepository.kt    # Сообщения чата
├── routes/
│   ├── AuthRoutes.kt        # /api/auth/*
│   ├── TaskRoutes.kt        # /api/tasks/*
│   └── AdminRoutes.kt       # /api/admin/*
├── security/
│   └── JwtConfig.kt         # JWT конфигурация
└── websocket/
    └── ChatWebSocket.kt     # WebSocket чат
```

## HTTP-коды

| Код | Описание |
|-----|----------|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden (не админ) |
| 404 | Not Found |
| 409 | Conflict (дубликат) |
| 500 | Internal Server Error |

## Развертывание

### Docker Hub
```bash
docker build -t your-username/ktor-final .
docker push your-username/ktor-final
```

### Heroku
```bash
heroku create your-app-name
heroku addons:create heroku-postgresql:mini
heroku config:set JWT_SECRET=your-production-secret
git push heroku main
```
