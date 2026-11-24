# Gym platform — Gym CRM, Trainer Workload and Eureka

A gym CRM system split into microservices:

| Service | Folder | Port | Role |
|---|---|---|---|
| **Gym CRM** | [`gym-crm/`](gym-crm) | 8080 | main REST API: trainees, trainers, trainings, security; PostgreSQL |
| **Trainer Workload** | [`trainer-workload-service/`](trainer-workload-service) | 8081 | each trainer's training minutes per month, in an in-memory database |
| **Discovery server** | [`discovery-server/`](discovery-server) | 8761 | Eureka registry: the services register and find each other by name |

Every time a training is added in Gym CRM, or deleted with its trainee, Gym CRM sends it to the Trainer Workload
service, which keeps each trainer's monthly summary. The services find each other through Eureka, Gym CRM authenticates
with a service JWT, and a circuit breaker with timeouts protects Gym CRM when the workload service is down.

Each service is a standalone Spring Boot project with its own `pom.xml`, `Dockerfile` and image, and can be built and run
on its own. The root `pom.xml` only aggregates them, and the root `compose.yml` runs them together.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Actuator, Security) |
| Microservices | Spring Cloud 2025.0: Netflix Eureka (server and client), LoadBalancer, CircuitBreaker with Resilience4j |
| Security | Spring Security 6, BCrypt, JWT (JJWT in Gym CRM; OAuth2 resource server in Trainer Workload) |
| Persistence | Gym CRM: Hibernate 6.6 (JPA), HikariCP, PostgreSQL. Trainer Workload: in-memory store |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus |
| API docs | OpenAPI 3 / Swagger UI (springdoc) in both API services |
| Logging | SLF4J + Logback, transaction id shared across services |
| Containers | Docker multi-stage builds, Docker Compose |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2, JaCoCo (≥ 80 % line coverage enforced) |

## Prerequisites

- JDK 17+ and Maven 3.9+ (or the ones bundled with IntelliJ IDEA)
- PostgreSQL to run Gym CRM outside Docker (tests use H2)
- or just **Docker** with Compose v2 to run everything in containers

## Building

All services, with their tests and coverage checks:

```bash
mvn clean verify
```

One service only:

```bash
mvn -f trainer-workload-service/pom.xml verify
```

## Running with Docker (recommended)

Only Docker is needed on the host. The four containers share the user-defined network `gym-net`.

| Compose service | Image | Container | Published port | Depends on |
|---|---|---|---|---|
| `db` | `postgres:17-alpine` | `gym-crm-db` | — | — |
| `discovery-server` | `gym-discovery-server:1.0` | `gym-discovery-server` | `EUREKA_PORT` (8761) | — |
| `trainer-workload-service` | `gym-trainer-workload-service:1.0` | `gym-trainer-workload-service` | `WORKLOAD_PORT` (8081) | `discovery-server` healthy |
| `gym-crm` | `gym-crm:1.0` | `gym-crm-app` | `APP_PORT` (8080) | `db` healthy, `discovery-server` healthy |

| File | Purpose |
|---|---|
| `*/Dockerfile` | one per service. Multi-stage build: Maven + JDK 17 builds the jar, a JRE 17 image (multi-arch, so it also runs on Apple Silicon) runs it as a non-root user, with a `HEALTHCHECK` on `/actuator/health` |
| `*/.dockerignore` | keeps `target/`, `.git/`, IDE files and `.env` out of each build context |
| `compose.yml` | the four services, health checks, start order, the `gym-net` network and the `gym-crm-db-data` volume |
| `.env.example` | template for the credentials, keys and ports Compose reads at startup |
| `application-docker.properties` | the `docker` Spring profile of each API service: settings come only from environment variables, logs go to stdout |

### Start

Create your `.env` from the template once. Set the database password and two **different** keys, generating each with
`openssl rand -base64 32`: `JWT_SECRET` for user tokens and `SERVICE_JWT_SECRET` for service-to-service tokens.

```bash
cp .env.example .env
```

Build the three images and start everything in the background:

```bash
docker compose up -d --build
```

Wait until every service shows `healthy`:

```bash
docker compose ps
```

The Eureka dashboard at <http://localhost:8761> lists `GYM-CRM` and `TRAINER-WORKLOAD-SERVICE`. As JSON:

```bash
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps
```

Health of each service:

```bash
curl http://localhost:8080/actuator/health
```

```bash
curl http://localhost:8081/actuator/health
```

```bash
curl http://localhost:8761/actuator/health
```

### Logs

Every service logs to standard output, so Docker's log commands show everything. Each line carries the transaction id.

```bash
docker compose logs -f gym-crm trainer-workload-service
```

```bash
docker logs gym-trainer-workload-service
```

### Stop and clean up

```bash
docker compose stop trainer-workload-service
```

```bash
docker compose start trainer-workload-service
```

To stop and remove everything, add `-v` to also delete the database volume:

```bash
docker compose down
```

### Configuration

No credentials, keys or addresses are stored in the Dockerfiles, the images or `compose.yml`. They are passed at
container startup as environment variables:

| Variable | Used by | Meaning |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Gym CRM | database (`DB_URL` is built by Compose from the service name `db`) |
| `JWT_SECRET` | Gym CRM | key of the user tokens |
| `SERVICE_JWT_SECRET` | Gym CRM, Trainer Workload | shared key of the service-to-service tokens |
| `EUREKA_URL` | Gym CRM, Trainer Workload | `http://discovery-server:8761/eureka/`, set in `compose.yml` |
| `EUREKA_HOSTNAME` | discovery server | `discovery-server`, its name on the network |
| `APP_PORT`, `WORKLOAD_PORT`, `EUREKA_PORT` | Compose | host ports |

The `docker` profile has no defaults for these: a service with a missing variable refuses to start and names it.

### Each service on its own

The images are independent. For example, the Trainer Workload service runs with plain Docker next to a registry:

```bash
docker network create gym-net
```

```bash
docker run -d --name discovery-server --network gym-net -p 8761:8761 -e EUREKA_HOSTNAME=discovery-server gym-discovery-server:1.0
```

```bash
docker run -d --name trainer-workload-service --network gym-net -p 8081:8081 -e SPRING_PROFILES_ACTIVE=docker -e EUREKA_URL=http://discovery-server:8761/eureka/ -e SERVICE_JWT_SECRET=<base64 key> gym-trainer-workload-service:1.0
```

```bash
docker logs -f trainer-workload-service
```

Gym CRM can also run without the workload service by setting `GYM_WORKLOAD_ENABLED=false`. Workload updates are then
skipped and only logged.

## Service communication

| Level | How | Example |
|---|---|---|
| Infrastructure | Compose service names, resolved by Docker's DNS on `gym-net` | `db:5432`, `discovery-server:8761` |
| Application | Eureka service names, resolved by Spring Cloud LoadBalancer | `http://trainer-workload-service/api/trainer-workloads` |

No container IP address is configured anywhere. Each service registers with Eureka using the IP Docker gave its
container at startup (`eureka.instance.prefer-ip-address=true`), and Gym CRM resolves `trainer-workload-service` to a
registered instance on every call. All addresses, ports and timeouts are runtime configuration (`application.properties`
and environment variables).

### Gym CRM → Trainer Workload

| Concern | Implementation |
|---|---|
| When | after a training is added (`ADD`), and for each training of a deleted trainee (`DELETE`): trainings are deleted only together with their trainee |
| Client | `RestTrainerWorkloadClient`: a `@LoadBalanced` `RestClient` with base URL `http://trainer-workload-service` |
| Authorization | `ServiceTokenProvider` issues a JWT signed with `SERVICE_JWT_SECRET` (issuer `gym-crm`, audience `trainer-workload-service`, valid for 5 minutes, reused until shortly before it expires). The workload service checks its signature, issuer, audience and expiry. User tokens are signed with another key, so neither kind works in place of the other. |
| Tracing | the request's transaction id is sent as `X-Transaction-Id`, so both services log the operation under the same id |
| Timeouts | connect `2s`, read `3s` (`gym.workload.connect-timeout`, `gym.workload.read-timeout`) |
| Circuit breaker | Resilience4j `trainerWorkload`: opens when at least half of the last 10 calls failed (minimum 5) or were slower than 2 s, stays open for 30 s, then lets 3 trial calls through. State: `GET /actuator/circuitbreakers` (needs a token). |
| Failure | any error (connection refused, timeout, error status, no registered instance, open circuit) gives `503 Service Unavailable` with `Retry-After: 30`. The operation's transaction is rolled back, so a training is never stored without its workload update, and a trainee is not deleted while its workload can't be updated. |

### Health and startup

- Every container has a health check on `/actuator/health` (PostgreSQL uses `pg_isready`). `docker compose ps` shows
  `healthy` only once the application has started, not just its container.
- Gym CRM and Trainer Workload wait for `discovery-server` to be **healthy** before they start, and Gym CRM also waits
  for `db`.
- If Eureka is restarted or starts late, the clients keep retrying and register again on their next heartbeat (every
  10 s). Gym CRM refreshes its registry copy every 5 s.
- Gym CRM does not wait for the Trainer Workload service. While that service is unavailable, the operations that need
  it fail fast with `503`, and everything else keeps working. Once the service is back, the circuit breaker's trial calls
  succeed and it closes again.

### Demo scenarios

Register a trainer and a trainee. Each response includes a bearer token:

```bash
curl -s -X POST http://localhost:8080/api/trainers -H "Content-Type: application/json" -d '{"firstName":"Nodira","lastName":"Karimova","specializationId":1}'
```

```bash
curl -s -X POST http://localhost:8080/api/trainees -H "Content-Type: application/json" -d '{"firstName":"John","lastName":"Doe"}'
```

Add a training as the trainee, with your own transaction id to follow it through both services:

```bash
curl -s -i -X POST http://localhost:8080/api/trainings -H "Authorization: Bearer <trainee token>" -H "X-Transaction-Id: demo-1" -H "Content-Type: application/json" -d '{"traineeUsername":"John.Doe","trainerUsername":"Nodira.Karimova","trainingName":"Morning Yoga","trainingDate":"2026-11-10","trainingDuration":60}'
```

```bash
docker compose logs gym-crm trainer-workload-service | grep demo-1
```

**Workload service unavailable.** Stop it and add trainings. The first calls fail after the connect timeout. Once the
circuit opens, `503` comes back at once and the service is no longer called:

```bash
docker compose stop trainer-workload-service
```

Start it again. About 30 s later the circuit lets trial calls through and trainings are accepted again:

```bash
docker compose start trainer-workload-service
```

**Eureka restart.** Both services register again within a few seconds:

```bash
docker compose restart discovery-server
```

## Running without Docker

Start the services in this order, each in its own terminal. The `local` profile (the default) uses Eureka on
`localhost:8761` and built-in development keys.

```bash
mvn -f discovery-server/pom.xml spring-boot:run
```

```bash
mvn -f trainer-workload-service/pom.xml spring-boot:run
```

```bash
mvn -f gym-crm/pom.xml spring-boot:run
```

Gym CRM needs PostgreSQL. Create the user and database once:

```bash
sudo -u postgres psql -c "CREATE USER gym_user WITH PASSWORD 'gym_pass';" -c "CREATE DATABASE gym_crm OWNER gym_user;"
```

Tables and the constant training types are created on startup from `gym-crm/src/main/resources/db/schema.sql` and
`db/data.sql`. Hibernate only validates the schema.

**Swagger UI:** <http://localhost:8080/swagger-ui.html> for Gym CRM (not in `prod`) and
<http://localhost:8081/swagger-ui.html> for Trainer Workload.

### Gym CRM environments (Spring profiles)

| Profile | Database | Notes |
|---|---|---|
| `local` *(default)* | `gym_crm` on localhost, credentials in `application-local.properties` | debug and SQL logging |
| `dev` | `gym_crm_dev` on localhost, `DB_*` variables with local defaults | debug logging |
| `stg` | from environment | log file in `logs/` |
| `prod` | from environment | log file in `logs/`, Swagger and health details off |
| `docker` | the PostgreSQL container | logs to stdout only |

`stg`, `prod` and `docker` have **no defaults**: if `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`SERVICE_JWT_SECRET` or `EUREKA_URL` is missing, Gym CRM refuses to start and names it.

## Trainer Workload service

### API

All endpoints need a service token (`Authorization: Bearer <JWT signed by Gym CRM>`).

| Operation | Method and path | Response |
|---|---|---|
| Add or delete a training | `POST /api/trainer-workloads` | `200 OK` |
| Trainer's summary by year and month | `GET /api/trainer-workloads/{username}` | `200`, or `404` for an unknown trainer |
| Trainer's minutes in one month | `GET /api/trainer-workloads/{username}/years/{year}/months/{month}` | `200` (0 for a month without trainings), `404` |

Request:

```json
{
  "trainerUsername": "Nodira.Karimova",
  "trainerFirstName": "Nodira",
  "trainerLastName": "Karimova",
  "isActive": true,
  "trainingDate": "2026-11-10",
  "trainingDuration": 60,
  "actionType": "ADD"
}
```

Summary (`trainingSummaryDuration` is in minutes):

```json
{
  "trainerUsername": "Nodira.Karimova",
  "trainerFirstName": "Nodira",
  "trainerLastName": "Karimova",
  "isActive": true,
  "years": [
    { "year": 2026, "months": [ { "month": 11, "trainingSummaryDuration": 60 } ] }
  ]
}
```

### Behavior

- `ADD` adds the duration to the training's month, creating the trainer, year and month as needed. The trainer's name
  and status are updated to the values sent with each training.
- `DELETE` subtracts the duration. A month that drops to zero is removed, and so is a year without months. Deleting from
  a month without workload changes nothing and is logged as a warning.
- Errors use the same JSON shape as Gym CRM: `400` for invalid fields, an invalid month or a malformed body, `401` for a
  missing or invalid token, `404`, `405`, `415`. Unexpected errors are logged and answered without internal details.
- The store (`InMemoryTrainerWorkloadRepository`) keeps workloads in memory, so they are lost on restart. A stored
  workload is never modified: an update works on a copy that is swapped in atomically, so concurrent updates of one
  trainer don't lose each other's changes.

## Gym CRM

### Security

Spring Security protects every endpoint except **trainee/trainer registration**, the API docs and the public actuator
endpoints (`health`, `info`, `prometheus`). The API is stateless: no session, no cookies, so CSRF protection is off.

| Feature | How |
|---|---|
| Password storage | BCrypt (`BCryptPasswordEncoder`): a random salt per password, stored inside the hash. The generated password is returned once at registration; only the hash is saved. |
| Login | `GET /api/auth/login` with the username/password in an HTTP Basic header. A dedicated filter chain authenticates them through Spring Security's `AuthenticationManager` and the response carries a JWT. |
| Authorization | Every other call sends `Authorization: Bearer <token>`. Tokens are HMAC-SHA256 signed, valid for 1 hour (`gym.security.jwt.expiration`). Registration also returns a token, so a new user doesn't have to log in. |
| Brute force protection | 3 wrong passwords in a row block the username for 5 minutes (`gym.security.brute-force.*`), even for the right password: `429 Too Many Requests` with `Retry-After`. A successful login resets the counter. |
| Logout | `POST /api/auth/logout` (Spring Security logout filter) revokes the token it was sent with; the token is refused afterwards. |
| CORS | Origins from `gym.security.cors.allowed-origins` (env `CORS_ALLOWED_ORIGINS`, default `http://localhost:3000,http://localhost:4200`), methods `GET POST PUT PATCH DELETE OPTIONS`, headers `Authorization`, `Content-Type`, `X-Transaction-Id`. |

```bash
curl -u John.Doe:aB3dE5gH7j http://localhost:8080/api/auth/login
```

Profile data can be viewed by any logged-in user, but only the owner can change it (`403` otherwise). Users created
before passwords were hashed have plain-text passwords, which no longer match: recreate the database or register them
again.

### Endpoints

| # | Operation | Method and path | Auth |
|---|---|---|---|
| 1 | Trainee registration | `POST /api/trainees` | — |
| 2 | Trainer registration | `POST /api/trainers` | — |
| 3 | Login | `GET /api/auth/login` | Basic → returns token |
| 4 | Change password | `PUT /api/auth/password` | owner + old password in body |
| — | Logout | `POST /api/auth/logout` | Bearer |
| 5 | Get trainee profile | `GET /api/trainees/{username}` | Bearer |
| 6 | Update trainee profile | `PUT /api/trainees/{username}` | owner |
| 7 | Delete trainee profile (and its trainings, also from trainer workloads) | `DELETE /api/trainees/{username}` | owner |
| 8 | Get trainer profile | `GET /api/trainers/{username}` | Bearer |
| 9 | Update trainer profile | `PUT /api/trainers/{username}` | owner |
| 10 | Active trainers not assigned to trainee | `GET /api/trainees/{username}/unassigned-trainers` | owner |
| 11 | Update trainee's trainers list | `PUT /api/trainees/{username}/trainers` | owner |
| 12 | Trainee trainings | `GET /api/trainees/{username}/trainings?periodFrom=&periodTo=&trainerName=&trainingType=` | owner |
| 13 | Trainer trainings | `GET /api/trainers/{username}/trainings?periodFrom=&periodTo=&traineeName=` | owner |
| 14 | Add training (also updates the trainer's workload) | `POST /api/trainings` | the training's trainee or trainer |
| 15 | Activate / deactivate trainee | `PATCH /api/trainees/{username}/status` | owner |
| 16 | Activate / deactivate trainer | `PATCH /api/trainers/{username}/status` | owner |
| 17 | Training types | `GET /api/training-types` | Bearer |

"owner" means a valid bearer token of the user named in the path. Dates use ISO format (`2026-11-10`), and training
durations are in minutes. Registration returns `201 Created`; all other successful calls return `200 OK`.

### Errors

Every error, in both API services, has the same JSON shape:

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
| 401 | Missing, invalid, expired or revoked token (`WWW-Authenticate: Bearer`); wrong login credentials (`WWW-Authenticate: Basic`) |
| 403 | Changing someone else's profile, or adding a training you're not part of |
| 404 | Unknown trainee, trainer, training type or URL |
| 405 / 415 | Wrong HTTP method / content type |
| 409 | Activating an already active profile (or deactivating an inactive one) |
| 429 | Login blocked for 5 minutes after 3 unsuccessful attempts |
| 503 | Trainer Workload service unavailable or its circuit breaker open: nothing was changed, retry later |
| 500 | Unexpected error: details are logged, never returned |

### Monitoring

| Endpoint | Content |
|---|---|
| `/actuator/health` | overall status and every health indicator (public) |
| `/actuator/info` | application description and Java version (public) |
| `/actuator/prometheus` | all metrics in Prometheus format, including Resilience4j ones (public) |
| `/actuator/metrics` | all metric names (token) |
| `/actuator/circuitbreakers` | state of the `trainerWorkload` circuit breaker (token) |

Custom health indicators: `trainingTypes`, `connectionPool`, `memory`. Custom metrics: `gym_registrations_total`,
`gym_authentications_total`, `gym_trainings_added_total`, `gym_trainings_search_seconds`, `gym_users_active`.
`monitoring/prometheus.yml` is a ready-to-use scrape configuration.

## Logging

Both API services log on two levels:

1. **Transaction level**: every request gets a transaction id, taken from an incoming `X-Transaction-Id` header or
   generated. It appears in every log line of the request, in the `X-Transaction-Id` response header and in error
   responses. Gym CRM passes it on to the Trainer Workload service, so one id follows an operation across both.
2. **Operation level**: one line per REST call with endpoint, status, duration and request/response bodies. The log
   level is `INFO` for success, `WARN` for 4xx and `ERROR` for 5xx. Passwords and tokens are masked.

## Tests

```bash
mvn clean verify
```

| Service | Tests | Line coverage | Highlights |
|---|---|---|---|
| Gym CRM | 265 | 97 % | unit, controller, DAO, security end-to-end, full API journey, and `TrainerWorkloadIntegrationTest`: a stub workload service registered under its service name checks the load-balanced call, service token, transaction id, timeout, rollback and circuit opening |
| Trainer Workload | 36 | 98 % | model and aggregation rules, concurrent updates of the in-memory store, API with real signed tokens (wrong key, issuer, audience, expired) |
| Discovery server | 2 | — | registry starts and serves the Eureka API |

JaCoCo fails the build of an API service if line coverage drops below 80 %. Reports are in
`<service>/target/site/jacoco/index.html`.

## Repository layout

```
.
├── pom.xml                         aggregator of the three services
├── compose.yml, .env.example       container environment
├── monitoring/prometheus.yml
├── discovery-server/               Eureka server (@EnableEurekaServer)
├── trainer-workload-service/
│   └── src/main/java/com/epam/trainerworkload
│       ├── model/                  TrainerWorkload → YearSummary → MonthSummary
│       ├── repository/             repository interface and in-memory implementation
│       ├── service/                ADD/DELETE rules, monthly summary
│       ├── security/               JWT resource server (signature, issuer, audience)
│       ├── config/                 logging filters, OpenAPI
│       └── web/                    controller, DTOs, error handler, logging filters
└── gym-crm/
    └── src/main/java/com/epam/gym
        ├── workload/               Trainer Workload client, circuit breaker, service token, wiring
        ├── security/               user authentication, JWT, brute force protection, logout, CORS
        ├── service/, dao/, model/  business logic and persistence
        ├── monitoring/             health indicators and metrics
        └── web/                    controllers, DTOs, mappers, error handler, logging filters
```

## Design notes

- **Deleting a trainee deletes its trainings**, and that is the only way a training is deleted, so it is when Gym CRM
  sends `DELETE`. All of the trainee's trainings are removed from the workloads.
- **Consistency without a message broker:** Gym CRM calls the workload service inside its own database transaction and
  rolls back if the call fails. A failure halfway through deleting a trainee with several trainings can still leave
  the workload partly updated. A message queue with retries (an outbox) would remove that gap.
- **One logging filter implementation per service:** the transaction id and REST logging filters are small and are
  copied rather than shared through a library, so the services stay independently buildable and deployable.
- **Username cannot change; one user, one role; trainer specialization is read-only** after registration.
- **Login keeps HTTP Basic** so credentials never appear in a URL or request log; everything after login uses JWTs.
- **Brute force state, revoked tokens and the workload store live in memory:** they reset on restart and are not
  shared between instances.
