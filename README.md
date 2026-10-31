# Gym CRM — REST API

REST API for a gym CRM system: trainees, trainers, their trainings and the trainer–trainee assignments.
Built with **Spring Core + Spring MVC (no Spring Boot)**, Hibernate 6 and PostgreSQL, running on embedded Tomcat.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Framework 6.2 (Core, MVC, ORM, Transactions) |
| Web server | Embedded Apache Tomcat 10.1 |
| Persistence | Hibernate 6.6 (JPA), HikariCP, PostgreSQL |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| API docs | OpenAPI 3 / Swagger UI (springdoc) |
| Logging | SLF4J + Logback |
| Tests | JUnit 5, Mockito, Spring MockMvc, H2 (in-memory) |

## Prerequisites

- JDK 17+
- Maven 3.9+ (or the one bundled with IntelliJ IDEA)
- PostgreSQL (only to run the application; tests use H2)

## Database setup

Create the database and user the application expects:

```bash
sudo -u postgres psql -c "CREATE USER gym_user WITH PASSWORD 'gym_pass';" -c "CREATE DATABASE gym_crm OWNER gym_user;"
```

Tables and the constant list of training types are created automatically on startup by
`src/main/resources/db/schema.sql` and `db/data.sql`. Hibernate only validates the schema (`hbm2ddl.auto=validate`).

## Configuration

Settings live in `src/main/resources/application.properties`:

| Property | Default | Meaning |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `db.url`, `db.username`, `db.password` | local `gym_crm` database | PostgreSQL connection |
| `password.length`, `password.characters` | `10`, letters and digits | Generated password format |
| `logging.rest.max-body-length` | `2000` | Max characters of request/response body written to the log |

Any property can be overridden with an environment variable, for example `DB_PASSWORD=secret` overrides `db.password`.

## Running

```bash
mvn compile exec:java
```

Or run `com.epam.gym.GymCrmApplication` from the IDE. The API starts on `http://localhost:8080`.

**Swagger UI:** <http://localhost:8080/swagger-ui.html> — use the **Authorize** button to enter a username and password.
The raw OpenAPI document is at `/v3/api-docs`.

## Authentication

Registration endpoints are public and return the generated username and password.
Every other endpoint requires **HTTP Basic** authentication:

```bash
curl -X POST http://localhost:8080/api/trainees \
     -H "Content-Type: application/json" \
     -d '{"firstName":"John","lastName":"Doe"}'
# {"username":"John.Doe","password":"aB3dE5gH7j"}

curl -u John.Doe:aB3dE5gH7j http://localhost:8080/api/trainees/John.Doe
```

Profile data can be viewed by any logged-in user, but only the owner can change it (`403` otherwise).

## Endpoints

| # | Operation | Method and path | Auth |
|---|---|---|---|
| 1 | Trainee registration | `POST /api/trainees` | — |
| 2 | Trainer registration | `POST /api/trainers` | — |
| 3 | Login | `GET /api/auth/login` | Basic |
| 4 | Change password | `PUT /api/auth/password` | old password in body |
| 5 | Get trainee profile | `GET /api/trainees/{username}` | Basic |
| 6 | Update trainee profile | `PUT /api/trainees/{username}` | owner |
| 7 | Delete trainee profile | `DELETE /api/trainees/{username}` | owner |
| 8 | Get trainer profile | `GET /api/trainers/{username}` | Basic |
| 9 | Update trainer profile | `PUT /api/trainers/{username}` | owner |
| 10 | Active trainers not assigned to trainee | `GET /api/trainees/{username}/unassigned-trainers` | owner |
| 11 | Update trainee's trainers list | `PUT /api/trainees/{username}/trainers` | owner |
| 12 | Trainee trainings | `GET /api/trainees/{username}/trainings?periodFrom=&periodTo=&trainerName=&trainingType=` | owner |
| 13 | Trainer trainings | `GET /api/trainers/{username}/trainings?periodFrom=&periodTo=&traineeName=` | owner |
| 14 | Add training | `POST /api/trainings` | the training's trainee or trainer |
| 15 | Activate / deactivate trainee | `PATCH /api/trainees/{username}/status` | owner |
| 16 | Activate / deactivate trainer | `PATCH /api/trainers/{username}/status` | owner |
| 17 | Training types | `GET /api/training-types` | Basic |

Dates use ISO format (`2026-11-10`). Training filters are optional; `trainerName` / `traineeName` take a username.
Registration returns `201 Created`; all other successful calls return `200 OK`.

## Errors

Every error has the same JSON shape:

```json
{
  "timestamp": "2026-11-01T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/trainees",
  "transactionId": "9b106831-757a-4c24-915b-633481e603d9",
  "violations": [{ "field": "lastName", "message": "must not be blank" }]
}
```

| Status | When |
|---|---|
| 400 | Missing/invalid fields, malformed JSON, invalid query parameter |
| 401 | Missing or wrong credentials (response includes `WWW-Authenticate: Basic`) |
| 403 | Changing someone else's profile, or adding a training you're not part of |
| 404 | Unknown trainee, trainer, training type or URL |
| 405 / 415 | Wrong HTTP method / content type |
| 409 | Activating an already active profile (or deactivating an inactive one) |
| 500 | Unexpected error — details are logged, never returned |

## Logging

Two levels, as required:

1. **Transaction level** — every request gets a transaction ID (taken from an incoming `X-Transaction-Id` header
   or generated). It appears in every log line of that request, in the `X-Transaction-Id` response header
   and in error responses, so it can be passed to downstream services.
2. **REST call level** — one line per call with method, path, status, duration and request/response bodies.
   Passwords are masked (`"password":"****"`).

```
INFO [9b106831-…] RestCallLoggingFilter - REST POST /api/trainees -> 201 in 46 ms | request: {"firstName":"John","lastName":"Doe"} | response: {"username":"John.Doe","password":"****"}
```

## Tests

```bash
mvn test
```

Runs 204 tests against an in-memory H2 database — no PostgreSQL needed:

- unit tests for services, generators, mappers, validation, filters and error handling (Mockito)
- controller tests with standalone MockMvc
- DAO tests against H2 with rollback after each test
- `GymCrmApiEndToEndTest` — full flow through all 17 endpoints with real transactions

## Project structure

```
src/main/java/com/epam/gym
├── GymCrmApplication.java   starts the Spring contexts and embedded Tomcat
├── config/                  root context: Spring, persistence, transactions
├── model/                   JPA entities
├── dao/                     data access (EntityManager, JPQL, Criteria API)
├── service/                 business logic, authentication, validation
├── dto/, exception/, util/  service-level types and helpers
└── web/                     web context
    ├── config/              Spring MVC, JSON and OpenAPI setup
    ├── controller/          REST controllers
    ├── dto/                 request and response bodies
    ├── mapper/              entity ↔ DTO conversion
    ├── auth/                HTTP Basic parsing, ownership check
    ├── error/               global exception handler
    └── logging/             transaction ID and REST call logging filters
```

## Design notes

- **Two Spring contexts:** the root context holds services and persistence; the web context (child) holds
  controllers and Spring MVC. Services never depend on the web layer.
- **Username cannot change:** it identifies the resource in the URL and is not part of any update body.
- **One user, one role:** every registration creates a new user, which belongs to exactly one trainee or trainer.
- **Trainer specialization is read-only** after registration; a training's type is the trainer's specialization.
- **Deleting a trainee** also deletes their trainings and trainer assignments.
- **Passwords are stored as plain text** in this module; hashing comes with Spring Security.
