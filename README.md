# Gym CRM — Spring Boot

REST API for a gym CRM system: trainees, trainers, their trainings and the trainer–trainee assignments.
Built with **Spring Boot 3.5**, Hibernate 6 and PostgreSQL, with Actuator health checks, Prometheus metrics
and separate configuration per environment.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Actuator) |
| Persistence | Hibernate 6.6 (JPA), HikariCP, PostgreSQL |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus |
| API docs | OpenAPI 3 / Swagger UI (springdoc) |
| Logging | SLF4J + Logback |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 (in-memory) |

## Prerequisites

- JDK 17+
- Maven 3.9+ (or the one bundled with IntelliJ IDEA)
- PostgreSQL (only to run the application; tests use H2)
- or just **Docker** with Compose v2 to run the application and database in containers

## Environments (Spring profiles)

| Profile | Database | Credentials | Notes |
|---|---|---|---|
| `local` *(default)* | `gym_crm` on localhost | in `application-local.properties` | debug logging, SQL logging, Swagger on |
| `dev` | `gym_crm_dev` on localhost | `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`, with local defaults | debug logging |
| `stg` | from environment | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` **required** | log file in `logs/` |
| `prod` | from environment | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` **required** | log file in `logs/`, Swagger and health details off |
| `docker` | the PostgreSQL container (host = service name `db`) | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` **required** | logs to stdout only — see [Running with Docker](#running-with-docker) |

Each environment also has its own connection-pool size, and every metric is tagged with its environment.
`stg`, `prod` and `docker` deliberately have **no default database settings**: if a variable is missing, the application
refuses to start and names it:

```
The following properties were declared as required but could not be resolved: [DB_URL, DB_USERNAME, DB_PASSWORD]
```

## Database setup

```bash
sudo -u postgres psql -c "CREATE USER gym_user WITH PASSWORD 'gym_pass';" -c "CREATE DATABASE gym_crm OWNER gym_user;"
```

For the `dev` profile, also create its own database:

```bash
sudo -u postgres psql -c "CREATE DATABASE gym_crm_dev OWNER gym_user;"
```

Tables and the constant list of training types are created on startup from `src/main/resources/db/schema.sql`
and `db/data.sql` (`spring.sql.init`). Hibernate only validates the schema.

## Running

```bash
mvn spring-boot:run
```

Starts with the `local` profile on `http://localhost:8080`. Another environment:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or build the executable jar and run it anywhere:

```bash
mvn clean package
```

```bash
DB_URL=jdbc:postgresql://db-host:5432/gym_crm DB_USERNAME=gym DB_PASSWORD=secret java -jar target/spring-boot-app-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

**Swagger UI** (not in `prod`): <http://localhost:8080/swagger-ui.html> — use **Authorize** to log in.

## Running with Docker

The application and PostgreSQL run in **separate containers** on a user-defined Docker network.
Only Docker is needed on the host (no JDK, Maven or PostgreSQL).

| File | Purpose |
|---|---|
| `Dockerfile` | multi-stage build: Maven + JDK 17 builds the jar, a JRE 17 image (multi-arch, so it also runs on Apple Silicon) runs it as a non-root user, with a `HEALTHCHECK` on `/actuator/health` |
| `.dockerignore` | keeps `target/`, `.git/`, IDE files and `.env` out of the build context |
| `docker-compose.yml` | `db` (PostgreSQL 17) and `app` services, the `gym-net` network and the `gym-crm-db-data` volume |
| `.env.example` | template for the credentials and ports Compose reads at startup |
| `application-docker.properties` | `docker` Spring profile: database settings **only** from `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, logs to stdout only |

**Configuration.** No credentials are stored in the `Dockerfile`, the image or `docker-compose.yml`. They are passed at
container startup as environment variables. Like `stg`/`prod`, the `docker` profile has no defaults and refuses to start
if one is missing. The database host in `DB_URL` is the Compose service name `db` (or the container name with plain
`docker run`), never `localhost` or an IP address.

### Docker Compose (recommended)

Create your `.env` from the template once and set a password:

```bash
cp .env.example .env
```

Build the image and start both containers in the background (the app waits until the database is healthy):

```bash
docker compose up -d --build
```

Check container status (`app` becomes `healthy` once Actuator reports `UP`):

```bash
docker compose ps
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Logs (all application logs go to standard output):

```bash
docker compose logs -f app
```

```bash
docker logs gym-crm-app
```

Shell inside the running container (the image has `sh` and `bash`):

```bash
docker exec -it gym-crm-app sh
```

Inspect the database directly:

```bash
docker exec -it gym-crm-db psql -U gym_user -d gym_crm -c "SELECT username, is_active FROM users;"
```

Stop / start / restart only the application (the database keeps running, data stays):

```bash
docker compose stop app
```

```bash
docker compose start app
```

```bash
docker compose restart app
```

Stop and remove everything (add `-v` to also delete the database volume):

```bash
docker compose down
```

### Plain Docker commands

The same environment without Compose:

```bash
docker build -t gym-crm:1.0 .
```

```bash
docker network create gym-net
```

```bash
docker run -d --name gym-crm-db --network gym-net -e POSTGRES_DB=gym_crm -e POSTGRES_USER=gym_user -e POSTGRES_PASSWORD=change_me -v gym-crm-db-data-plain:/var/lib/postgresql/data postgres:17-alpine
```

```bash
docker run -d --name gym-crm-app --network gym-net -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker -e DB_URL=jdbc:postgresql://gym-crm-db:5432/gym_crm -e DB_USERNAME=gym_user -e DB_PASSWORD=change_me gym-crm:1.0
```

```bash
docker ps
```

```bash
docker logs -f gym-crm-app
```

Clean up:

```bash
docker rm -f gym-crm-app gym-crm-db && docker network rm gym-net && docker volume rm gym-crm-db-data-plain
```

### Verifying store and retrieve

Register a trainee (stored in PostgreSQL), read it back, restart the app container and read it again:

```bash
curl -s -X POST http://localhost:8080/api/trainees -H "Content-Type: application/json" -d '{"firstName":"John","lastName":"Doe","address":"Main St 1"}'
# {"username":"John.Doe","password":"aB3dE5gH7j"}
```

```bash
curl -s -u John.Doe:aB3dE5gH7j http://localhost:8080/api/trainees/John.Doe
```

```bash
docker compose restart app
```

```bash
curl -s -u John.Doe:aB3dE5gH7j http://localhost:8080/api/trainees/John.Doe
```

The profile is still returned after the restart: the application reconnected to the database container and the data
lives in the `gym-crm-db-data` volume. HikariCP also recovers on its own when the **database** container is restarted
(`docker compose restart db`): requests fail briefly while it is down, then succeed again without restarting the app.

## Monitoring

Actuator endpoints:

| Endpoint | Content |
|---|---|
| `/actuator/health` | overall status and every health indicator |
| `/actuator/info` | application description and Java version |
| `/actuator/metrics` | all metric names |
| `/actuator/prometheus` | all metrics in Prometheus format |

**Custom health indicators**

| Indicator | DOWN when |
|---|---|
| `trainingTypes` | the constant training types are missing (trainers can't be registered) |
| `connectionPool` | requests are waiting for a database connection (pool exhausted) |
| `memory` | heap usage is above `gym.health.memory.max-heap-usage` (default 90%) |

**Custom Prometheus metrics** (all tagged with `application` and `environment`)

| Metric | Type | Tags |
|---|---|---|
| `gym_registrations_total` | counter | `role` = trainee / trainer |
| `gym_authentications_total` | counter | `result` = success / failure |
| `gym_trainings_added_total` | counter | — |
| `gym_trainings_search_seconds` | timer | `side` = trainee / trainer |
| `gym_users_active` | gauge | `role` = trainee / trainer |

`monitoring/prometheus.yml` is a ready-to-use scrape configuration for a Prometheus server.

## Authentication

Registration endpoints are public and return the generated username and password.
Every other API endpoint requires **HTTP Basic** authentication:

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

1. **Transaction level** — every request gets a transaction ID (from an incoming `X-Transaction-Id` header
   or generated). It appears in every log line of that request, in the `X-Transaction-Id` response header
   and in error responses.
2. **REST call level** — one line per call with method, path, status, duration and request/response bodies.
   Passwords are masked (`"password":"****"`).

Log levels are set per profile; `stg` and `prod` also write a rolling log file to `logs/gym-crm.log`
(`logback-spring.xml`).

## Tests

```bash
mvn test
```

Runs 230 tests against an in-memory H2 database (`test` profile) — no PostgreSQL needed:

- unit tests for services, generators, mappers, validation, filters, error handling, health indicators and metrics
- profile tests: each environment's database settings, and `stg`/`prod` refusing to start without them
- controller tests with standalone MockMvc
- DAO tests with `@DataJpaTest` (rolled back after each test)
- `ActuatorEndpointsTest` — health, info and Prometheus output of the running application
- `GymCrmApiEndToEndTest` — full flow through all 17 endpoints with real transactions

## Project structure

```
src/main/java/com/epam/gym
├── GymCrmApplication.java   @SpringBootApplication entry point
├── config/                  application beans, required-properties check for stg/prod
├── model/                   JPA entities
├── dao/                     data access (EntityManager, JPQL, Criteria API)
├── service/                 business logic, authentication, validation
├── monitoring/
│   ├── health/              custom health indicators
│   └── metrics/             custom Micrometer metrics
├── dto/, exception/, util/  service-level types and helpers
└── web/
    ├── config/              Spring MVC, filters and OpenAPI setup
    ├── controller/          REST controllers
    ├── dto/                 request and response bodies
    ├── mapper/              entity ↔ DTO conversion
    ├── auth/                HTTP Basic parsing, ownership check
    ├── error/               global exception handler
    └── logging/             transaction ID and REST call logging filters
```

## Design notes

- **Username cannot change:** it identifies the resource in the URL and is not part of any update body.
- **One user, one role:** every registration creates a new user, which belongs to exactly one trainee or trainer.
- **Trainer specialization is read-only** after registration; a training's type is the trainer's specialization.
- **Deleting a trainee** also deletes their trainings and trainer assignments.
- **Actuator endpoints are not behind HTTP Basic:** they serve monitoring systems, not users, and would normally
  be protected at the network level (or with Spring Security in the next module).
- **Passwords are stored as plain text** in this module; hashing comes with Spring Security.
