# Spring Boot Migration Notes

Date: 2026-05-26

## Goal

Move `home-budgeting-assistant` from Spring Boot 2.5.1 to Spring Boot 4 while keeping the application reactive with WebFlux and replacing JPA/H2 access with R2DBC/H2 access.

## Main Migration Challenge

The most important challenge was not only the technical upgrade itself, but the working constraint: the code was not
changed manually during the migration. The migration was driven by instructing AI to make each change, then reviewing,
correcting, and iterating through further instructions until the application, tests, and generated API contract aligned.

## Current State

- Spring Boot parent: `4.0.6`.
- Java bytecode target: `21`.
- Maven Wrapper is present and pinned to Maven `3.9.15`.
- Maven Enforcer requires Java `21+` and Maven `3.9+`.
- Configuration uses `application.yml`.
- The server port is deployment-friendly through `server.port=${PORT:8080}`.
- A `demo` Spring profile uses ephemeral in-memory H2 storage for free public hosting.
- Lombok remains pinned to `1.18.46`.
- Application web stack is still WebFlux.
- Persistence uses Spring Data R2DBC repositories.
- Database migration still uses Liquibase.
- API interfaces and request/response DTOs are generated from `src/main/resources/openapi/home-budget-api.yaml`.

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

The demo profile uses the same shared in-memory database name through each driver grammar:

```yaml
spring:
  r2dbc:
    url: "r2dbc:h2:mem:///demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
  liquibase:
    url: "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
```

This keeps the deployed demo stateless enough for free hosts such as Render Free or Koyeb Free. The database disappears
when the process restarts, and `POST /demo/reset` restores the seed rows during a running demo. The reset controller
and reset service are profile-gated with `@Profile("demo")`, so the endpoint is not exposed in the default local
file-backed profile.

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

## OpenAPI Contract Generation

The API contract is now checked in at `src/main/resources/openapi/home-budget-api.yaml`.
`openapi-generator-maven-plugin:7.6.0` generates:

- WebFlux API interfaces in `pl.mm.homebudget.api`.
- Request and response DTO classes in `pl.mm.homebudget.api.dto`.

The handwritten controllers implement those generated interfaces and keep only implementation logic. DTO source files
under `src/main/java/pl/mm/homebudget/api/dto` were removed; schema and validation changes should be made in the
OpenAPI file first.

The generator uses `useResponseEntity=false` so WebFlux signatures stay direct (`Flux<T>` or `Mono<T>`) instead of
nested wrappers such as `Mono<ResponseEntity<Flux<T>>>`. Create operations still set the `Location` header through
`ServerWebExchange`.

## Verification

Run with JDK 21:

```powershell
$env:JAVA_HOME='C:\Users\mmaciejewski\.jdks\temurin-21.0.11'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

Current result:

- Build: success.
- Unit tests: no tests matched the current Surefire naming pattern.
- Integration tests: 41 passed.

Additional checks performed:

- No remaining `javax.validation` imports.
- No remaining `org.webjars.NotFoundException` usage.
- No RestAssured dependency in the filtered dependency tree.
- No JPA/Hibernate dependency in the filtered dependency tree.

## Remaining Warnings

- Springdoc logs that `/v3/api-docs` and `/swagger-ui.html` are enabled by default. This is expected unless those endpoints should be disabled in a production profile.
- OpenAPI Generator 7.6.0 flattens response media types into generated Spring `@RequestMapping(produces = ...)`
  declarations. Because error responses use `application/problem+json`, some generated success mappings may also
  advertise `application/problem+json` even though the checked-in OpenAPI YAML correctly documents success responses as
  `application/json` and errors as `application/problem+json`. This is accepted for now to avoid adding a custom
  generator template or an extra generated-source post-processing plugin only for media type narrowing.

## Completed Follow-Up Hardening

- Added `@NotNull` next to `@Positive` on request amount fields, with validation tests for `null` amounts.
- Same-register transfers now fail with `400 Bad Request`, with service and integration coverage.
- Configured Mockito as an explicit test JVM agent and disabled test JVM class sharing to remove future-JDK agent warnings.
- Configured Lombok as an explicit annotation processor to remove javac's implicit annotation-processing warning.
- Added a resource-oriented JSON API:
  - `GET /registers`
  - `GET /registers/{registerId}`
  - `GET /operations`
  - `GET /operations/{operationId}`
  - `POST /operations/recharges`
  - `POST /operations/transfers`
- Added public demo reset endpoint `POST /demo/reset` behind the `demo` profile; it clears operation history, restores
  the four active seed registers, and returns the restored register list.
- Added WebFlux CORS configuration controlled by `app.cors.allowed-origins` / `APP_CORS_ALLOWED_ORIGINS`; default local
  frontend origins are allowed, while wildcard origins are avoided.
- Removed old `POST /registers/recharge` and `POST /registers/transfer` endpoints after replacing them with the
  resource-oriented API.
- Reorganized Java packages around API, application, domain, persistence, and configuration responsibilities.
- Renamed the Java package root from `com.solera.budgeting` to `pl.mm.homebudget` and Maven group from `com.solera` to `pl.mm`.
- Renamed the Spring Boot entry point to `HomeBudgetApplication`.
- Replaced handwritten API DTO records and controller OpenAPI annotations with OpenAPI-generated WebFlux interfaces
  and DTO classes.
- Kept Swagger UI and `/v3/api-docs` enabled because this is a demo/showcase project rather than a production service.
- Replaced brittle JSON substring assertions in `RegisterDbIT` with JSONAssert-backed structural assertions.
- Standardized validation and domain API errors on `application/problem+json` `ProblemDetail` responses.
- Mapped the OpenAPI `ProblemDetail` schema to Spring's `org.springframework.http.ProblemDetail` so no redundant
  `ProblemDetail` DTO is generated; the validation `errors` extension uses the generated `ValidationError` DTO.
- Kept register path IDs as human-readable register names for now. This fits the current demo/showcase API because
  registers are seeded, not user-managed, and there is no register rename workflow. If register management grows later,
  stable URL-safe slugs should be preferred over exposing mutable display names as resource IDs.
- Added money amount precision and scale validation at the OpenAPI boundary. Request amounts remain generated as
  `BigDecimal`, are positive, and now reject values with more than 17 integer digits or more than two decimal places
  through generated `@Digits(integer = 17, fraction = 2)` validation. The OpenAPI schema documents cent-level
  granularity with `multipleOf: 0.01`; a large numeric `maximum` was intentionally not used because the generator rounded
  `99999999999999999.99` to scientific notation.

## Manual Liquibase Plugin Usage

`src/main/resources/db/liquibase-plugin.properties` is intentionally kept for manual Liquibase Maven plugin operations
against a chosen database file. It is separate from the application runtime configuration in `application.yml`.
