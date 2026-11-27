# Gym platform — Gym CRM, Trainer Workload, ActiveMQ and Eureka

A gym CRM system split into microservices:

| Service | Folder | Port | Role |
|---|---|---|---|
| **Gym CRM** | [`gym-crm/`](gym-crm) | 8080 | main REST API: trainees, trainers, trainings, security; PostgreSQL |
| **Trainer Workload** | [`trainer-workload-service/`](trainer-workload-service) | 8081 | each trainer's training minutes per month; MongoDB |
| **ActiveMQ** | image `apache/activemq-classic` | 61616, console 8161 | message broker between the two services |
| **MongoDB** | image `mongo:8.0` | 27017 (not published) | Trainer Workload's database |
| **Discovery server** | [`discovery-server/`](discovery-server) | 8761 | Eureka registry the services register with |

Every time a training is added in Gym CRM, or deleted with its trainee, Gym CRM puts a message on the
`trainer.workload` queue. The Trainer Workload service consumes it and updates the trainer's monthly summary.
The communication is asynchronous: Gym CRM doesn't wait for the workload service and keeps working while it is down.
Each message carries a service JWT and the transaction id. Messages that can't be processed end in a dead letter queue.

Each service is a standalone Spring Boot project with its own `pom.xml`, `Dockerfile` and image, and can be built and run
on its own. The root `pom.xml` only aggregates them, and the root `compose.yml` runs them together.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Actuator, Security) |
| Messaging | ActiveMQ Classic 6.1, Spring JMS (`JmsTemplate`, `@JmsListener`), JSON messages (Jackson) |
| Microservices | Spring Cloud 2025.0: Netflix Eureka (server and client), CircuitBreaker with Resilience4j |
| Security | Spring Security 6, BCrypt, JWT (JJWT in Gym CRM; OAuth2 resource server in Trainer Workload) |
| Persistence | Gym CRM: Hibernate 6.6 (JPA), HikariCP, PostgreSQL. Trainer Workload: Spring Data MongoDB, MongoDB 8 |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus |
| API docs | OpenAPI 3 / Swagger UI (springdoc) in both API services |
| Logging | SLF4J + Logback, transaction id shared across services |
| Containers | Docker multi-stage builds, Docker Compose |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2, embedded ActiveMQ, Testcontainers (MongoDB), Awaitility, JaCoCo (≥ 80 % line coverage enforced) |

## Prerequisites

- JDK 17+ and Maven 3.9+ (or the ones bundled with IntelliJ IDEA)
- PostgreSQL to run Gym CRM outside Docker (tests use H2)
- **Docker**: the Trainer Workload tests start a MongoDB container (Testcontainers), and Docker Compose runs everything

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

Only Docker is needed on the host. The six containers share the user-defined network `gym-net`.

| Compose service | Image | Container | Published port | Depends on |
|---|---|---|---|---|
| `db` | `postgres:17-alpine` | `gym-crm-db` | — | — |
| `mongodb` | `mongo:8.0` | `gym-mongodb` | — | — |
| `activemq` | `apache/activemq-classic:6.1.8` | `gym-activemq` | `ACTIVEMQ_CONSOLE_PORT` (8161) | — |
| `discovery-server` | `gym-discovery-server:1.0` | `gym-discovery-server` | `EUREKA_PORT` (8761) | — |
| `trainer-workload-service` | `gym-trainer-workload-service:1.0` | `gym-trainer-workload-service` | `WORKLOAD_PORT` (8081) | `mongodb`, `discovery-server`, `activemq` healthy |
| `gym-crm` | `gym-crm:1.0` | `gym-crm-app` | `APP_PORT` (8080) | `db`, `discovery-server`, `activemq` healthy |

| File | Purpose |
|---|---|
| `*/Dockerfile` | one per service. Multi-stage build: Maven + JDK 17 builds the jar, a JRE 17 image (multi-arch, so it also runs on Apple Silicon) runs it as a non-root user, with a `HEALTHCHECK` on `/actuator/health` |
| `*/.dockerignore` | keeps `target/`, `.git/`, IDE files and `.env` out of each build context |
| `compose.yml` | the six services, health checks, start order, the `gym-net` network, and the `gym-crm-db-data`, `gym-mongo-data` and `gym-activemq-data` volumes |
| `.env.example` | template for the credentials, keys and ports Compose reads at startup |
| `application-docker.properties` | the `docker` Spring profile of each API service: settings come only from environment variables, logs go to stdout |

### Start

Create your `.env` from the template once. Set the PostgreSQL, MongoDB and broker passwords and two **different** keys, generating
each with `openssl rand -base64 32`: `JWT_SECRET` for user tokens and `SERVICE_JWT_SECRET` for service-to-service tokens.

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

The ActiveMQ console at <http://localhost:8161/admin> (user `admin`, password `ACTIVEMQ_PASSWORD`) shows the
`trainer.workload` queue with its consumers and the `DLQ.trainer.workload` dead letter queue.

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
| `ACTIVEMQ_URL` | Gym CRM, Trainer Workload | `tcp://activemq:61616?connectionTimeout=3000`, set in `compose.yml` |
| `ACTIVEMQ_USER`, `ACTIVEMQ_PASSWORD` | ActiveMQ, Gym CRM, Trainer Workload | broker login (the broker requires it) |
| `MONGO_USERNAME`, `MONGO_PASSWORD` | MongoDB | root user; Compose builds the workload service's `MONGODB_URI` from them (host `mongodb`). Use only letters and digits in the password. |
| `EUREKA_HOSTNAME` | discovery server | `discovery-server`, its name on the network |
| `APP_PORT`, `WORKLOAD_PORT`, `EUREKA_PORT`, `ACTIVEMQ_CONSOLE_PORT` | Compose | host ports |

The `docker` profile has no defaults for these: a service with a missing variable refuses to start and names it.

### Each service on its own

The images are independent. For example, the Trainer Workload service runs with plain Docker next to a registry,
a broker and MongoDB:

```bash
docker network create gym-net
```

```bash
docker run -d --name discovery-server --network gym-net -p 8761:8761 -e EUREKA_HOSTNAME=discovery-server gym-discovery-server:1.0
```

```bash
docker run -d --name mongodb --network gym-net -e MONGO_INITDB_ROOT_USERNAME=workload -e MONGO_INITDB_ROOT_PASSWORD=<password> mongo:8.0
```

```bash
docker run -d --name activemq --network gym-net -p 8161:8161 -e ACTIVEMQ_CONNECTION_USER=gym -e ACTIVEMQ_CONNECTION_PASSWORD=<password> apache/activemq-classic:6.1.8
```

```bash
docker run -d --name trainer-workload-service --network gym-net -p 8081:8081 -e SPRING_PROFILES_ACTIVE=docker -e EUREKA_URL=http://discovery-server:8761/eureka/ -e SERVICE_JWT_SECRET=<base64 key> -e ACTIVEMQ_URL=tcp://activemq:61616 -e ACTIVEMQ_USER=gym -e ACTIVEMQ_PASSWORD=<password> -e MONGODB_URI='mongodb://workload:<password>@mongodb:27017/trainer_workload?authSource=admin' gym-trainer-workload-service:1.0
```

```bash
docker logs -f trainer-workload-service
```

Gym CRM can also run without any workload messaging by setting `GYM_WORKLOAD_ENABLED=false`. Workload updates are then
skipped and only logged.

## Messaging

Gym CRM and Trainer Workload don't call each other: they communicate only through ActiveMQ. All integration goes
through Spring JMS (`JmsTemplate` to send, `@JmsListener` to receive, Spring Boot's connection factory and listener
container), with no hand-written JMS connections, sessions or consumers.

| | |
|---|---|
| Queue | `trainer.workload` (`gym.workload.queue`, `workload.messaging.queue`) |
| Dead letter queue | `DLQ.trainer.workload` (`workload.messaging.dead-letter-queue`) |
| Body | JSON text message: `trainerUsername`, `trainerFirstName`, `trainerLastName`, `isActive`, `trainingDate`, `trainingDuration` (minutes), `actionType` (`ADD`/`DELETE`) |
| Properties | `authorization` (`Bearer <service JWT>`), `transactionId`, `JMSXGroupID` (trainer username), `_type` (`trainerWorkload`) |

### Sending (Gym CRM)

| Concern | Implementation |
|---|---|
| When | after a training is added (`ADD`), and for each training of a deleted trainee (`DELETE`): trainings are deleted only together with their trainee |
| Serialization | `MappingJackson2MessageConverter`: JSON text messages; the type travels as the logical name `trainerWorkload`, not a Java class name |
| Consistency | `JmsTemplate` uses a transacted session (`spring.jms.template.session.transacted=true`) that Spring commits only after the database transaction commits: a rolled-back operation sends nothing, and a trainee deletion sends all its messages or none |
| Authorization | `ServiceTokenProvider` issues a JWT signed with `SERVICE_JWT_SECRET` (issuer `gym-crm`, audience `trainer-workload-service`, valid for 5 minutes). The consumer checks its signature, issuer, audience and expiry. |
| Tracing | the request's transaction id is sent as the `transactionId` property, so both services log the operation under the same id |
| Ordering | `JMSXGroupID` = trainer username: ActiveMQ gives all of a trainer's messages to one consumer at a time, in order |
| Broker down | the broker URL has `connectionTimeout=3000`, and a Resilience4j circuit breaker (`trainerWorkload`) opens when at least half of the last 10 sends (minimum 5) failed or took over 2 s. Either way the request answers `503` with `Retry-After: 30`, and the database change is rolled back. |

### Receiving (Trainer Workload)

| Situation | What happens |
|---|---|
| Valid message | the workload is updated; the message is acknowledged |
| Invalid message: missing or bad token, malformed JSON, missing or invalid fields | moved to `DLQ.trainer.workload` at once with a `deadLetterReason` property (for example `Invalid fields: trainingDate must not be null`), the original queue and the transaction id. The token is not copied. Logged at `WARN`. |
| Processing fails | the transacted session rolls back and ActiveMQ redelivers it, after 1 s, 2 s, 4 s (`workload.messaging.redelivery-delay`). On the 4th attempt (`workload.messaging.max-delivery-attempts`) it is moved to the dead letter queue with the reason. Logged at `WARN`, the final failure at `ERROR`. |
| Workload service down | messages wait in the queue (persistent, on the `gym-activemq-data` volume) and are processed when it is back. Gym CRM is not affected. |

The body is read by `WorkloadMessageReader` (token, JSON, Bean Validation) rather than by the container's converter, so
a malformed message becomes a dead letter with a reason instead of being retried pointlessly. Moving a message to the
dead letter queue and acknowledging it happen in the same transacted session.

### Horizontal scaling

- **Concurrent consumers:** the listener container runs `workload.messaging.concurrency` consumers (`min-max`, added
  and removed with the load): `1-2` locally, `2-5` in `dev` and `docker`, `3-10` in `stg`, `5-20` in `prod`.
- **Competing consumers:** it is a queue, so each message goes to exactly one consumer, whether the consumers are
  threads of one instance or several instances.
- **Per-trainer ordering:** message groups (`JMSXGroupID`) keep one trainer's messages on one consumer at a time, so a
  `DELETE` is never handled before the `ADD` it cancels.
- **Shared store:** every instance and consumer works on the same MongoDB collection. Message groups serialize one
  trainer's updates, and the document's `@Version` turns any concurrent update that slips through into a retry
  (redelivery) instead of a lost update. A unique index on `username` stops two consumers from creating the same
  trainer twice.

### Configuration per environment (profiles)

| Profile | Broker | Credentials | Consumers |
|---|---|---|---|
| `local` *(default)* | `tcp://localhost:61616` | `admin` / `admin` | 1-2 |
| `dev` | `ACTIVEMQ_URL`, default localhost | `ACTIVEMQ_USER` / `ACTIVEMQ_PASSWORD`, default `admin` | 2-5 |
| `stg`, `prod`, `docker` | `ACTIVEMQ_URL` **required** | `ACTIVEMQ_USER` / `ACTIVEMQ_PASSWORD` **required** | 3-10, 5-20, 2-5 |
| `test` | embedded in-memory broker (`vm://`) | — | default |

### Health and startup

- Every container has a health check: `/actuator/health` for the Spring services, `pg_isready` for PostgreSQL, a
  `ping` command for MongoDB, and the OpenWire port for ActiveMQ. `docker compose ps` shows `healthy` only once the application is ready, not just its
  container.
- Gym CRM and Trainer Workload wait for `activemq` and `discovery-server` to be **healthy**. Gym CRM also waits for
  `db`, and Trainer Workload for `mongodb`.
- If the broker restarts, both services reconnect by themselves. If Eureka restarts, they register again on their next
  heartbeat (every 10 s).

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

**Workload service down.** Stop it and add trainings: Gym CRM still answers `200`, and the messages wait in the queue
(visible in the console). Start it again and it processes them:

```bash
docker compose stop trainer-workload-service
```

```bash
docker compose start trainer-workload-service
```

**Dead letter queue.** Send an invalid message with ActiveMQ's own producer (it has no service token). It shows up in
`DLQ.trainer.workload` with the reason `Missing service token`:

```bash
docker exec gym-activemq /opt/apache-activemq/bin/activemq producer --brokerUrl tcp://localhost:61616 --user gym --password <ACTIVEMQ_PASSWORD> --destination queue://trainer.workload --messageCount 1 --message '{"trainerUsername":"Nodira.Karimova"}'
```

**Broker down.** Stop ActiveMQ and add a training: Gym CRM answers `503` and saves nothing. After repeated failures the
circuit breaker opens and `503` comes back at once:

```bash
docker compose stop activemq
```

## Running without Docker

Start a local broker and MongoDB first, then the services in this order, each in its own terminal. The `local`
profile (the default) uses ActiveMQ on `localhost:61616` (`admin`/`admin`), MongoDB on `localhost:27017` (database
`trainer_workload`, no authentication), Eureka on `localhost:8761` and built-in development keys.

```bash
docker run -d --name activemq -p 61616:61616 -p 8161:8161 apache/activemq-classic:6.1.8
```

```bash
docker run -d --name mongodb -p 27017:27017 mongo:8.0
```

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
`SERVICE_JWT_SECRET`, `EUREKA_URL`, `ACTIVEMQ_URL`, `ACTIVEMQ_USER` or `ACTIVEMQ_PASSWORD` is missing, Gym CRM refuses
to start and names it.

## Trainer Workload service

### API

Workload changes arrive only as messages (see [Messaging](#messaging)). The REST API is read-only, and all endpoints
need a service token (`Authorization: Bearer <JWT signed by Gym CRM>`).

| Operation | Method and path | Response |
|---|---|---|
| Trainers' summaries by name | `GET /api/trainer-workloads?firstName=&lastName=` | `200` with a list (empty if nobody matches), `400` if a name is missing |
| Trainer's summary by year and month | `GET /api/trainer-workloads/{username}` | `200`, or `404` for an unknown trainer |
| Trainer's minutes in one month | `GET /api/trainer-workloads/{username}/years/{year}/months/{month}` | `200` (0 for a month without trainings), `404` |

Message body:

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

### MongoDB schema

One document per trainer in the `trainer_workloads` collection (`TrainerWorkload` with embedded `YearSummary` and
`MonthSummary`):

```json
{
  "_id": ObjectId("..."),
  "version": 2,
  "username": "Nodira.Karimova",
  "firstName": "Nodira",
  "lastName": "Karimova",
  "status": true,
  "years": [
    { "year": 2026, "months": [ { "month": 11, "trainingSummaryDuration": 120 } ] }
  ]
}
```

| Field | Type | Rules |
|---|---|---|
| `username` | string | required, **unique index** `trainer_username_idx` |
| `firstName`, `lastName` | string | required, **compound index** `trainer_name_idx` for the search by name |
| `status` | boolean | required: trainer is active |
| `years[].year` | number | 1900–9999 |
| `years[].months[].month` | number | 1–12 |
| `years[].months[].trainingSummaryDuration` | number (Int64) | minutes, ≥ 0 |
| `version` | number | optimistic locking (`@Version`) |

The indexes are declared on the document class and created at startup (`spring.data.mongodb.auto-index-creation`).
The Bean Validation rules are checked before every save (`ValidatingMongoEventListener`), and every incoming message
is validated before it reaches the service.

### Repository and service

`TrainerWorkloadRepository` is a Spring Data `MongoRepository`: `findByUsername` finds the document, `save` updates it,
and `findByFirstNameAndLastName` uses the name index. `TrainerWorkloadServiceImpl` handles each message:

1. Look the trainer's document up by username.
2. If there is none, create it with the trainer's data and a year and month taken from the training date.
3. Find the summary of the training's year and month, creating it if needed.
4. `ADD`: add the training duration to its summary duration. `DELETE`: subtract it; a month that drops to zero is
   removed, and so is a year without months. Deleting from a month without workload changes nothing and is logged as
   a warning.
5. Update the trainer's name and status to the values in the message, and save the document.

Workloads now survive restarts, and REST errors use the same JSON shape as Gym CRM: `400` for an invalid year, month or
missing name, `401` for a missing or invalid token, `404`, `405`. Unexpected errors are logged and answered without
internal details.

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
| 503 | The workload message could not be queued (broker down or circuit breaker open): nothing was changed, retry later |
| 500 | Unexpected error: details are logged, never returned |

### Monitoring

| Endpoint | Content |
|---|---|
| `/actuator/health` | overall status and every health indicator (public) |
| `/actuator/info` | application description and Java version (public) |
| `/actuator/prometheus` | all metrics in Prometheus format, including Resilience4j ones (public) |
| `/actuator/metrics` | all metric names (token) |
| `/actuator/circuitbreakers` | state of the `trainerWorkload` circuit breaker around the broker (token) |

Custom health indicators: `trainingTypes`, `connectionPool`, `memory`. Custom metrics: `gym_registrations_total`,
`gym_authentications_total`, `gym_trainings_added_total`, `gym_trainings_search_seconds`, `gym_users_active`.
`monitoring/prometheus.yml` is a ready-to-use scrape configuration.

## Logging

Both API services log on two levels:

1. **Transaction level**: every request gets a transaction id, taken from an incoming `X-Transaction-Id` header or
   generated. It appears in every log line of the request, in the `X-Transaction-Id` response header and in error
   responses. Gym CRM passes it on in each message's `transactionId` property, and the workload listener logs under
   it, so one id follows an operation across both services.
2. **Operation level**: one line per REST call with endpoint, status, duration and request/response bodies. The log
   level is `INFO` for success, `WARN` for 4xx and `ERROR` for 5xx. Passwords and tokens are masked. Queued and
   processed messages are logged at `INFO`, dead letters and retries at `WARN`, final failures at `ERROR`. Tokens are
   never logged.

## Tests

```bash
mvn clean verify
```

Builds the three services and runs their unit, integration and Cucumber component tests. Docker must be running, because
the Trainer Workload tests start a MongoDB container.

```bash
mvn clean verify -Pintegration-tests
```

Also runs the Cucumber integration tests of the whole platform (see below). They build the images and start the Compose
stack, so they take a few minutes.

| Service | Tests | Line coverage | Highlights |
|---|---|---|---|
| Gym CRM | 286, including 22 Cucumber scenarios | 97 % | unit, controller, DAO, security end-to-end, full API journey; on an embedded broker, `WorkloadMessagingIntegrationTest` checks the JSON body, token, transaction id and message group, and that a rolled-back transaction sends nothing; `BrokerUnavailableIntegrationTest` checks the `503`, the rollback and the circuit opening |
| Trainer Workload | 76, including 18 Cucumber scenarios | 97 % | model and aggregation rules, service flow with a mocked repository; on a MongoDB container (Testcontainers), the repository: search and update by username, search by name, both indexes, unique username, validation before saving, optimistic locking; read API with real signed tokens; message reader and listener unit tests; on an embedded broker, valid messages, dead letters for missing fields and missing token, redelivery after a transient failure, and dead-lettering after 4 failed attempts |
| Discovery server | 2 | — | registry starts and serves the Eureka API |
| Platform (`integration-tests`) | 9 Cucumber scenarios | — | both services together on the real containers |

JaCoCo fails the build of an API service if line coverage drops below 80 %. Reports are in
`<service>/target/site/jacoco/index.html`, and Cucumber reports in `<module>/target/cucumber-reports/*.html`.

### BDD with Cucumber

The behavior is described in Gherkin feature files (`Given` / `When` / `Then`), each with positive and negative
scenarios, and run with Cucumber 7 on the JUnit Platform (`@Suite` runners). Scenarios are independent: each starts
from its own accounts, an emptied queue or database, and a fresh scenario-scoped state.

**Component tests** run one service at a time: the whole service (REST API, security, persistence, JMS), with its
neighbors replaced by test doubles.

| Service | Feature files (`src/test/resources/features/component/`) | Test doubles |
|---|---|---|
| Gym CRM | `registration`: registering, duplicate names, missing fields, unknown specialization. `security`: login, wrong password, blocking after 3 failures, logout, missing token. `profiles`: own vs someone else's profile, unknown trainee. `trainings`: the ADD message with token and transaction id, DELETE messages when a trainee is deleted, a training for someone else, invalid trainings | H2 instead of PostgreSQL; an embedded broker whose queue the scenarios read in place of the Trainer Workload service |
| Trainer Workload | `workload_messages`: creating the document, adding up by month and year, cancelling, unknown trainer, dead letters for a missing or foreign token, missing fields, an invalid duration, an unknown action, malformed JSON. `workload_api`: summary, one month, empty month, search by name, unknown trainer, invalid month, missing token | the scenarios play Gym CRM on an embedded broker; MongoDB is real, in a container |

**Integration tests** (`integration-tests/`, `features/integration/`) check the microservices together, as black boxes
over HTTP. `Platform` starts compose.yml with the Docker Compose CLI: images built from the current sources, secrets
generated for the run, random free host ports, and `up --wait` until every health check passes. It uses its own project
name plus `compose.it.yml` (own container, network and volume names), so a development stack is never touched, and it
removes everything at the end.

| Feature | Scenarios |
|---|---|
| `workload_sync` | both services registered in Eureka; a training added in Gym CRM shows up in the trainer's monthly workload; trainings add up per month; deleting the trainee removes them; the transaction id appears in both services' logs; an invalid training or one added by an outsider is rejected and never counted |
| `resilience` | trainings added while the Trainer Workload service is stopped are counted once it is back; while the broker is stopped Gym CRM answers `503` and saves nothing, and works again once the broker is back |

## Repository layout

```
.
├── pom.xml                         aggregator of the three services (+ integration-tests with -Pintegration-tests)
├── compose.yml, .env.example       container environment (PostgreSQL, MongoDB, ActiveMQ, Eureka, both services)
├── monitoring/prometheus.yml
├── integration-tests/              Cucumber integration tests on the Compose stack (compose.it.yml override)
├── discovery-server/               Eureka server (@EnableEurekaServer)
├── trainer-workload-service/
│   └── src/main/java/com/epam/trainerworkload
│       ├── model/                  MongoDB document: TrainerWorkload → YearSummary → MonthSummary
│       ├── repository/             Spring Data MongoDB repository
│       ├── service/                ADD/DELETE rules, monthly summary
│       ├── messaging/              JMS listener, message reader, dead letter publisher, redelivery
│       ├── security/               service JWT checks (signature, issuer, audience)
│       ├── config/                 logging filters, OpenAPI, document validation
│       └── web/                    read-only controller, DTOs, error handler, logging filters
└── gym-crm/
    └── src/main/java/com/epam/gym
        ├── workload/               JMS publisher, JSON converter, circuit breaker, service token
        ├── security/               user authentication, JWT, brute force protection, logout, CORS
        ├── service/, dao/, model/  business logic and persistence
        ├── monitoring/             health indicators and metrics
        └── web/                    controllers, DTOs, mappers, error handler, logging filters
```

## Design notes

- **Deleting a trainee deletes its trainings**, and that is the only way a training is deleted, so it is when Gym CRM
  sends `DELETE`. All of the trainee's trainings are removed from the workloads.
- **Consistency:** messages are sent in a JMS transaction that commits right after the database transaction, so they
  are sent only for committed changes, and all together. The remaining gap is a broker failure in the moment between
  the two commits, which would lose the messages; a transactional outbox would close it.
- **Dead letters are explicit:** the listener routes invalid and repeatedly failing messages to the dead letter queue
  itself, with a reason, instead of relying on broker configuration. The client's redelivery limit is one higher, so
  the broker's `ActiveMQ.DLQ` is only a last-resort fallback.
- **One logging filter implementation per service:** the transaction id and REST logging filters are small and are
  copied rather than shared through a library, so the services stay independently buildable and deployable.
- **Username cannot change; one user, one role; trainer specialization is read-only** after registration.
- **Login keeps HTTP Basic** so credentials never appear in a URL or request log; everything after login uses JWTs.
- **Brute force state and revoked tokens live in memory:** they reset on restart and are not shared between
  instances. Trainer workloads are stored in MongoDB.
