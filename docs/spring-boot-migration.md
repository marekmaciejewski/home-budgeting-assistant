# Spring Boot Migration Notes

Date: 2026-05-26

## Goal

Move `home-budgeting-assistant` from Spring Boot 2.5.1 to Spring Boot 4 while keeping the application reactive with WebFlux and replacing JPA/H2 access with R2DBC/H2 access.

## Current State

- Spring Boot parent: `4.0.6`.
- Java bytecode target: `21`.
- Maven Wrapper is present and pinned to Maven `3.9.15`.
- Maven Enforcer requires Java `21+` and Maven `3.9+`.
- Configuration uses `application.yml`.
- Lombok remains pinned to `1.18.46`.
- Application web stack is still WebFlux.
- Persistence uses Spring Data R2DBC repositories.
- Database migration still uses Liquibase.

## Staged Path Taken

1. Added Maven Wrapper and build environment enforcement.
2. Moved the build to JDK 21 while Spring Boot 2.x still built Java 8 bytecode.
3. Converted `application.properties` to `application.yml`.
4. Upgraded Spring Boot `2.5.1` to `2.7.18`.
5. Removed explicit `jakarta.validation-api` and let Boot manage validation.
6. Converted persistence from JPA/H2 to R2DBC/H2.
7. Upgraded through Spring Boot `3.5.14`.
8. Upgraded to Spring Boot `4.0.6`.

## Persistence Decisions

The application now uses:

- `spring-boot-starter-data-r2dbc`
- `io.r2dbc:r2dbc-h2`
- `com.h2database:h2`
- `spring-boot-starter-liquibase`

Liquibase is JDBC-based, so the application keeps a JDBC H2 driver available even though the runtime repositories use R2DBC. In the current dependency graph, `r2dbc-h2` also brings `com.h2database:h2` transitively, but the explicit H2 dependency is kept because Liquibase directly relies on the JDBC driver.

The application has two database URLs by design:

```yaml
spring:
  r2dbc:
    url: "r2dbc:h2:file:///[replace this with your db directory];DB_CLOSE_ON_EXIT=FALSE"
  liquibase:
    url: "jdbc:h2:file:[replace this with your db directory];DB_CLOSE_ON_EXIT=FALSE"
```

The test configuration uses the same shared in-memory database name through each driver grammar:

```yaml
spring:
  r2dbc:
    url: "r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
  liquibase:
    url: "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
```

The slash difference is intentional. R2DBC and JDBC use different H2 URL grammars, and attempts to make the URLs visually identical caused either invalid URLs or separate in-memory databases.

## Register And Operation Relationship

The database still models the relationship with foreign keys from `OPERATION` to `REGISTER`.

The Java model no longer uses JPA object graphs, cascades, or lazy collection loading. `Operation` stores source and target register IDs, and operations are persisted explicitly through `OperationRepository`. This matches Spring Data R2DBC's aggregate style and avoids pretending that R2DBC has JPA relationship management.

## Boot 4 Adjustments

- Validation imports moved from `javax.validation` to `jakarta.validation`.
- Springdoc moved to `springdoc-openapi-starter-webflux-ui:3.0.3`.
- `org.webjars.NotFoundException` was replaced with an application-owned `RegisterNotFoundException`.
- Test dependency changed to `spring-boot-starter-webflux-test`.
- RestAssured was removed from tests.
- Integration tests now use `WebTestClient`.
- `@MockBean` was replaced with Spring Test's `@MockitoBean`.
- In `RegisterControllerIT`, the mocked service is declared at class level:

```java
@MockitoBean(types = RegisterService.class)
@WebFluxTest(RegisterController.class)
class RegisterControllerIT {
```

## Verification

Run with JDK 21:

```powershell
$env:JAVA_HOME='C:\Users\mmaciejewski\.jdks\temurin-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

Current result:

- Build: success.
- Unit tests: 24 passed.
- Integration tests: 26 passed.

Additional checks performed:

- No remaining `javax.validation` imports.
- No remaining `org.webjars.NotFoundException` usage.
- No RestAssured dependency in the filtered dependency tree.
- No JPA/Hibernate dependency in the filtered dependency tree.

## Remaining Warnings

- Springdoc logs that `/v3/api-docs` and `/swagger-ui.html` are enabled by default. This is expected unless those endpoints should be disabled in a production profile.

## Possible Follow-Up Work

- Decide whether Swagger UI and API docs should be profile-gated.
- Consider replacing register names in URLs with stable technical IDs or slugs if this stops being a prototype API.

## Completed Follow-Up Hardening

- Added `@NotNull` next to `@Positive` on request amount fields, with validation tests for `null` amounts.
- Same-register transfers now fail with `400 Bad Request`, with service and integration coverage.
- Configured Mockito as an explicit test JVM agent and disabled test JVM class sharing to remove future-JDK agent warnings.
- Configured Lombok as an explicit annotation processor to remove javac's implicit annotation-processing warning.
- Added a resource-oriented JSON API:
  - `GET /registers`
  - `GET /registers/{registerId}`
  - `POST /registers/{registerId}/recharges`
  - `POST /transfers`
  - `GET /operations`
  - `GET /operations/{operationId}`
- Removed old `POST /registers/recharge` and `POST /registers/transfer` endpoints after replacing them with the
  resource-oriented API.

## Manual Liquibase Plugin Usage

`src/main/resources/db/liquibase-plugin.properties` is intentionally kept for manual Liquibase Maven plugin operations
against a chosen database file. It is separate from the application runtime configuration in `application.yml`.
