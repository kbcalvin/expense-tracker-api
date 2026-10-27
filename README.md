# Ledger — Expense Tracker API

A portfolio project combining a **Java 21 / Spring Boot REST API** with a responsive browser interface for recording expenses and viewing category totals.

## Tech stack

- Java 21, Spring Boot 3.2.5, Spring Web, Spring Data JPA, Spring Security
- PostgreSQL and Flyway SQL migrations
- JWT authentication (`jjwt`), BCrypt password hashing
- HTML, CSS and vanilla JavaScript frontend served by Spring Boot
- Maven and Docker

## Implemented features

- Register an account and log in with email and password
- Receive a JWT for authenticated API requests
- Create and list expenses belonging to the authenticated user
- Group expenses by category and calculate category totals
- Browser interface for account access and expense tracking
- Health-check endpoint

**Current scope:** The API source includes expense creation and retrieval, but **does not implement update or delete endpoints**. No automated tests were included in the uploaded project.

## Requirements

- JDK 21
- Maven 3.9+
- PostgreSQL running locally (default port `5432`)

## Run locally

1. Create a PostgreSQL database named `expenses`.
2. Configure the environment variables below. Spring Boot reads environment variables directly; `.env.example` is a reference and **is not automatically loaded by Maven**.
3. From the project root run:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your_local_database_password"
$env:JWT_SECRET = "replace-with-a-unique-random-secret-at-least-32-bytes-long"
mvn spring-boot:run
```

Open `http://localhost:8082/`. Flyway runs the included initial database migration. The health endpoint is `http://localhost:8082/api/health`.

Optional configuration: `DB_PORT` (default `5432`) and `DB_NAME` (default `expenses`). The application server uses port `8082`.

> **Security:** Generate your own strong JWT secret; never publish a real secret, database password or JWT. If a previous JWT secret was used beyond local development, rotate it.

## API reference

| Method | Path | Authentication | Purpose |
| --- | --- | --- | --- |
| GET | `/api/health` | No | Returns `{ "status": "up" }` |
| POST | `/api/auth/register` | No | Register an email and password; returns token and email |
| POST | `/api/auth/login` | No | Log in; returns token |
| GET | `/api/expenses` | Bearer JWT | List the signed-in user's expenses |
| POST | `/api/expenses` | Bearer JWT | Create an expense for the signed-in user |
| GET | `/api/expenses/summary` | Bearer JWT | Totals grouped by category |

### Example requests

Register:

```http
POST /api/auth/register
Content-Type: application/json

{"email":"demo@example.com","password":"choose-a-strong-password"}
```

Create an expense using the token from registration/login:

```http
POST /api/expenses
Authorization: Bearer YOUR_TOKEN
Content-Type: application/json

{"title":"Groceries","amount":25.50,"category":"Food","date":"2026-10-26"}
```

Summary response shape (example):

```json
{"Food":25.5}
```

## Project structure

```text
src/main/java/com/calvin/expensetracker/
  controller/     API controllers
  model/          JPA entities
  repository/     Spring Data repositories
  security/       JWT filter and security configuration
src/main/resources/
  application.properties
  db/migration/   Flyway SQL migrations
  static/         HTML, CSS and JavaScript UI
pom.xml
Dockerfile
.env.example
```

## Build and Docker

```powershell
mvn clean package
docker build -t ledger-expense-tracker .
```

The Docker image requires access to a PostgreSQL database and the same environment variables; `localhost` inside a container does not refer to the host computer. Configure a reachable database address before deploying.

## Improvements to consider

- Add automated integration and security tests
- Validate expense inputs and restrict negative or invalid amounts
- Add update/delete expense endpoints with ownership checks
- Add pagination and API documentation
- Add secure deployment configuration and rate limiting

## Author

Kudakwashe Calvin Bamusi — [GitHub](https://github.com/kbcalvin)

Built as a learning and software engineering portfolio project, with AI-assisted development.
