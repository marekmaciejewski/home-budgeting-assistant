# Agent Notes

## Project Snapshot

- Java package root: `pl.mm.homebudget`.
- Spring Boot entry point: `pl.mm.homebudget.HomeBudgetApplication`.
- Build tool: Maven Wrapper.
- Runtime baseline: Java 21, Spring Boot 4.0.6.
- Web stack: Spring WebFlux. Keep the reactive style unless explicitly asked otherwise.
- Persistence stack: Spring Data R2DBC with H2.
- Schema migration: Liquibase through JDBC/H2.

## Commands

Use JDK 21 before running Maven:

```powershell
$env:JAVA_HOME='C:\Users\mmaciejewski\.jdks\temurin-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Run all checks:

```powershell
.\mvnw.cmd clean verify
```

Run unit tests only:

```powershell
.\mvnw.cmd test -DskipITs
```

Run the app:

```powershell
.\mvnw.cmd spring-boot:run
```

## Current Package Layout

```text
pl.mm.homebudget
  api
    dto
    error
    operation
    register
    transfer
  application
  config
  domain
  persistence
    entity
```

- `api`: controllers, request/response DTOs, and HTTP exception handling.
- `application`: use-case orchestration, currently `RegisterService`.
- `domain`: business exceptions.
- `persistence`: R2DBC repositories, persistence entities, and persistence-to-API conversion.
- `config`: Spring configuration.

Do not move toward a full hexagonal split unless the domain model grows enough to justify separating pure domain objects from R2DBC entities.

## API Shape

Primary endpoints:

- `GET /registers`
- `GET /registers/{registerId}`
- `POST /registers/{registerId}/recharges`
- `POST /transfers`
- `GET /operations`
- `GET /operations/{operationId}`

Removed legacy endpoints:

- `POST /registers/recharge`
- `POST /registers/transfer`

Do not reintroduce those old endpoints as compatibility aliases unless the user explicitly asks.

Register IDs are currently register names. Names containing spaces must be URL-encoded when used in path variables.

## Database Notes

The app intentionally has two H2 URLs:

- `spring.r2dbc.url` for reactive application access.
- `spring.liquibase.url` for JDBC Liquibase migrations.

Both URLs must point to the same database. R2DBC and JDBC H2 URL grammars are different; do not try to make them visually identical.

Liquibase plugin configuration lives in:

```text
src/main/resources/db/liquibase-plugin.properties
```

That file is for manual Liquibase Maven plugin use, not the runtime app connection.

## Testing Notes

- Unit tests live under matching packages in `src/test/java/pl/mm/homebudget`.
- Integration tests use `WebTestClient`.
- Controller slice tests use `@MockitoBean`, not the old `@MockBean`.
- RestAssured was removed; do not reintroduce it for new tests.
- Mockito is configured as an explicit Java agent in `pom.xml`.
- Lombok is configured as an explicit annotation processor in `pom.xml`.

Known expected warning:

- Springdoc logs that `/v3/api-docs` and `/swagger-ui.html` are enabled by default. This is expected until API docs are profile-gated.

## Style And Constraints

- Keep changes scoped to the current task.
- Prefer existing package boundaries and naming patterns.
- Keep REST endpoints resource-oriented.
- Keep JSON assertions structural where possible; avoid brittle substring checks for new tests.
- Do not reintroduce JPA/Hibernate. R2DBC entities are the current persistence model.
- Update `docs/spring-boot-migration.md` when changing migration-relevant decisions.
