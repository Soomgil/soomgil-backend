# Soomgil Backend

Spring Boot backend for Soomgil.

## Stack

- Java 21
- Spring Boot 3.5.15
- Gradle
- PostgreSQL
- Redis
- Flyway
- MyBatis
- Spring Security
- Spring Modulith

## Run

Requirements:

- JDK 21
- Docker Desktop or a compatible Docker runtime

Recommended local runtime:

- macOS: Docker Desktop or Colima
- Windows: Docker Desktop with WSL2 backend

```bash
cp .env.example .env
./gradlew bootRun
```

On Windows PowerShell:

```powershell
copy .env.example .env
.\gradlew.bat bootRun
```

Spring Boot Docker Compose support starts `compose.yaml` automatically when Docker is running.

Useful endpoints:

- `GET /api/v1/health`
- `GET /actuator/health`
- `GET /swagger-ui`
- Mailpit: `http://localhost:8025`
- MinIO console: `http://localhost:9001`

## Local Infrastructure

macOS/Linux:

```bash
colima start
docker compose up -d
docker compose ps
docker compose down
```

Windows PowerShell:

```powershell
docker compose up -d
docker compose ps
docker compose down
```

The compose stack contains PostgreSQL, Redis, Mailpit, and MinIO.

## Test

```bash
./gradlew test
```

On Windows PowerShell:

```powershell
.\gradlew.bat test
```

The generated context test uses Testcontainers, so Docker must be available.
When using Colima, the Gradle test task automatically sets the Docker socket environment if `~/.colima/default/docker.sock` exists. If you run tests outside Gradle, use:

```bash
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## CQRS-lite Structure

Each domain package follows the same shape:

```text
{domain}/
  api/
  application/
    command/
      dto/
      handler/
    query/
      dto/
      handler/
  domain/
    model/
    policy/
    event/
  infrastructure/
    persistence/
      mapper/
      repository/
      row/
    external/
```

Rules:

- Controllers call command handlers or query handlers.
- Command handlers own writes and transactions.
- Query handlers own read-only lookup and projection access.
- Domain code does not depend on API, persistence, MyBatis, or external clients.
- Application code may depend on infrastructure repository interfaces/classes directly in this lite structure.
- Infrastructure repositories use MyBatis mappers and row records.
- Use `common.cqrs.Command`, `CommandHandler`, `Query`, and `QueryHandler` when a use case benefits from explicit typing.

## MyBatis Persistence

Use this shape for SQL-backed persistence:

```text
{domain}/
  application/
    command/
      dto/
        CreateTripCommand.java
      handler/
        CreateTripHandler.java
    query/
      dto/
        FindTripDetailQuery.java
        TripDetailView.java
      handler/
        FindTripDetailHandler.java
  infrastructure/
    persistence/
      mapper/
        TripCommandMapper.java
        TripQueryMapper.java
      repository/
        TripCommandRepository.java
        TripQueryRepository.java
      row/
        TripRow.java

src/main/resources/mappers/{domain}/
  TripCommandMapper.xml
  TripQueryMapper.xml
```

Guidelines:

- Mapper interfaces are annotated with `@Mapper`.
- XML SQL lives under `src/main/resources/mappers/{domain}/`.
- `row` records represent database rows, not domain models.
- Repository implementations translate between domain/view DTOs and rows.
- Command handlers use command repositories for writes.
- Query handlers use query repositories for reads.
- Do not introduce JPA entities, `EntityManager`, Hibernate repositories, or `spring-boot-starter-data-jpa`.

## Preference Resources

Preference and recommendation seed inputs live under `src/main/resources/preference/`.

```text
src/main/resources/preference/
  tags/
    # Fixed tag dictionary seed. Must match .agent/docs/product-specs/preference_tagging_policy.md.
  personas/
    # 50 fixed cold-start persona definitions for synthetic swipe generation.
```

Rules:

- Tag dictionary seed data must use only the fixed whitelist from the product policy.
- Persona definitions must keep exactly 50 active personas per generator version.
- Synthetic persona swipe data must stay separate from real user swipe events.

## Tourism Source Resources

Tourism source import and award-photo matching resources live under `src/main/resources/tourism-source/`.

```text
src/main/resources/tourism-source/
  imports/
    # Manifests for KTO/SSAFY-style sidos, guguns, contenttypes, attractions, attraction image imports.
  award-photos/
    # S3 object metadata manifests for downloaded KTO Contents Lab award photos.
  matching-rules/
    # Region/file-name alias rules used before manual matching.
```

Rules:

- Source tourism data is for enrichment/tag extraction and is not the production service place master.
- Award photo binaries are uploaded to S3-compatible storage, not committed to the repository.
- Award photos may match exact attractions, region only, or remain unmatched for future use.

## Naming

- `Command`: request data for an operation that changes state.
- `CommandHandler`: one focused class that handles one command and owns the write transaction.
- `Query`: request data for an operation that reads state.
- `QueryHandler`: one focused class that handles one query and owns the read use case.
- `Repository`: persistence-facing class that calls MyBatis mappers.
- `Mapper`: MyBatis interface connected to XML SQL.
- `Row`: database row shape.

Use the same action name across a command/query, its handler, and its result/view:

```text
CreateTripCommand -> CreateTripHandler -> CreateTripResult
InviteTripMemberCommand -> InviteTripMemberHandler -> InviteTripMemberResult
FindTripDetailQuery -> FindTripDetailHandler -> TripDetailView
FindRecommendedPlacesQuery -> FindRecommendedPlacesHandler -> PageResponse<RecommendedPlaceView>
```

Handler return rules:

- Commands that create or change state return an explicit `{Action}Result` record.
- Commands that do not need a response body return `NoResult`, not `Void` or `null`.
- Queries that return one object use `{Resource}View`.
- Queries that return a list use `PageResponse<{Resource}View>` or another agreed page response type.
- API response DTOs stay in `api/dto`; application handlers return result/view records, not HTTP DTOs.

## Common Contracts

- CQRS handler generic order is input first, result second: `CommandHandler<CreateTripCommand, CreateTripResult>` and `QueryHandler<FindTripQuery, TripView>`.
- Use `NoResult` for command handlers that intentionally produce no response body.
- Controllers and application services should use `ProblemDetails` for common error response shape.
- `CurrentUserProvider` is the minimal security contract for domain code that needs the authenticated user id.
- Tests can use a fake `CurrentUserProvider` without waiting for full auth implementation.
- Domain events shared across modules use `EventEnvelope<T>` with `eventId`, `eventType`, `schemaVersion`, `occurredAt`, aggregate metadata, optional `actorUserId`, and typed payload.
- S3/MinIO metadata shared across modules uses `StorageObjectKey` and `StorageObjectMetadata`; object keys are relative, forward-slash paths and `publicUrl` is optional.
- Use `Ids`, `TimeProvider`, and `ValidationRules` for shared UUID parsing, testable current time, and repeated domain invariants.
