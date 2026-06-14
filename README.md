# Soomgil Backend

Spring Boot backend for Soomgil.

## Stack

- Java 21
- Spring Boot 3.5.15
- Gradle
- PostgreSQL
- Redis
- Flyway
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
      service/
    port/
      in/
      out/
  domain/
    model/
    policy/
    event/
  infrastructure/
    persistence/
      entity/
      repository/
      mapper/
    external/
```

Rules:

- Controllers call command handlers or query services.
- Command handlers own writes and transactions.
- Query services own read-only lookup and projection access.
- Domain code does not depend on API, persistence, or external clients.
- Infrastructure implements outbound ports.
- Use `common.cqrs.Command`, `CommandHandler`, `Query`, and `QueryHandler` when a use case benefits from explicit typing.
