# home-budgeting-assistant

## Preparation

Use JDK 21 when building or running the application.

The default profile keeps the local file-backed H2 setup. Before the application can be started in the default profile,
an H2 database file path must be provided in `application.yml` for both the R2DBC application connection and the JDBC
Liquibase migration connection. The URLs should point at the same database file:

```yaml
spring:
  r2dbc:
    url: "r2dbc:h2:file:///C:/Users/you/foo/bar/db/registers;DB_CLOSE_ON_EXIT=FALSE"
  liquibase:
    url: "jdbc:h2:file:C:/Users/you/foo/bar/db/registers;DB_CLOSE_ON_EXIT=FALSE"
```

The server port is configured as `server.port=${PORT:8080}`, so free hosting providers can inject their assigned port
with the `PORT` environment variable.

## Instruction

The app can be run from IDE or maven plugin: `.\mvnw.cmd spring-boot:run`.
The DB will be loaded with initial state of following registers:

| register         | balance |
|------------------|---------|
| Wallet           | 1000    |
| Savings          | 5000    |
| Insurance policy | 0       |
| Food expenses    | 0       |

In the default file-backed profile, deleting the local DB file gives a fully fresh database on the next start.

## Demo profile

For free demo hosting, run with the `demo` profile:

```powershell
$env:SPRING_PROFILES_ACTIVE='demo'
.\mvnw.cmd spring-boot:run
```

The `demo` profile uses a named in-memory H2 database shared by Liquibase JDBC and R2DBC:

```yaml
spring:
  r2dbc:
    url: "r2dbc:h2:mem:///demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
  liquibase:
    url: "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
```

This is intentionally ephemeral: data is reset when the backend process restarts, and users can also reset it with
`POST /demo/reset`. The reset endpoint and its supporting service are only active when the `demo` profile is enabled.

Deployment-relevant environment variables:

| variable                   | example                                      | purpose                                                |
|----------------------------|----------------------------------------------|--------------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`   | `demo`                                       | enables ephemeral demo H2 storage                      |
| `PORT`                     | provided by Render/Koyeb                     | host-provided server port, defaults to `8080` locally  |
| `APP_CORS_ALLOWED_ORIGINS` | `https://your-user.github.io,http://localhost:5173` | comma-separated frontend origins allowed by CORS |

By default, CORS allows local Vite development origins: `http://localhost:5173` and `http://127.0.0.1:5173`. Add the
GitHub Pages origin through `APP_CORS_ALLOWED_ORIGINS` before exposing the hosted frontend.

## Live demo

- API base URL: `https://home-budgeting-assistant.onrender.com`
- Swagger UI: `https://home-budgeting-assistant.onrender.com/swagger-ui/index.html`

The hosted backend runs on Render Free. Render can spin down idle services, so the first request after inactivity may
take about a minute before the app responds.

The hosted backend uses the `demo` profile with ephemeral in-memory H2. Restart, redeploy, or idle spin-down starts from
the seeded state, and `POST /demo/reset` restores the seed data during a running demo.

## Interact

The app exposes the following primary API on the default host:

| method | url                                  | description                         |
|--------|--------------------------------------|-------------------------------------|
| GET    | `/registers`                         | get all active registers            |
| GET    | `/registers/{registerId}`            | get one active register             |
| GET    | `/operations`                        | get all balance-changing operations |
| GET    | `/operations/{operationId}`          | get one balance-changing operation  |
| POST   | `/operations/recharges`              | create a recharge operation         |
| POST   | `/operations/transfers`              | create a transfer operation         |
| POST   | `/demo/reset`                        | reset public demo state; demo profile only |

Register IDs are currently register names, so names containing spaces must be URL-encoded in path variables.

The details of the input/output model are available through Swagger UI. Locally, open
`http://localhost:8080/swagger-ui.html`; for the hosted backend, use the live demo link above.

The checked-in OpenAPI contract lives in `src/main/resources/openapi/home-budget-api.yaml`.
Maven generates the Spring WebFlux API interfaces and request/response DTOs from that file during the build.

## Sample requests

### POST /operations/recharges

```json
{
  "registerId": "Wallet",
  "amount": 2500
}
```

### POST /operations/transfers

```json
{
  "sourceRegisterId": "Wallet",
  "targetRegisterId": "Food expenses",
  "amount": 1500
}
```

### GET /registers

```json
[
  {
    "id": "Wallet",
    "balance": 1000.00
  },
  {
    "id": "Savings",
    "balance": 5000.00
  }
]
```

### POST /demo/reset

Available only with `SPRING_PROFILES_ACTIVE=demo`. Clears operation history, restores the seed registers, and returns
the restored register list:

```json
[
  {
    "id": "Wallet",
    "balance": 1000.00
  },
  {
    "id": "Savings",
    "balance": 5000.00
  },
  {
    "id": "Insurance policy",
    "balance": 0.00
  },
  {
    "id": "Food expenses",
    "balance": 0.00
  }
]
```

## Tests

To run all the unit and integration tests run command: `.\mvnw.cmd clean verify`.

The same command generates a JaCoCo coverage report at `target/site/jacoco/index.html`.
In GitHub Actions, the `Coverage` workflow shows a coverage table in the job summary and uploads the full HTML report as
the `jacoco-coverage-report` artifact.

## The Original Assignment

### Background

As a member of a small team that has been tasked with quickly assembling a prototype of a small web application for an
upcoming customer demo, you were made responsible for designing and implementing its back-end part.

Meant as home budgeting aid, the planned system will need to expose multiple endpoints to support all desired
functionalities. For now, however, it has been decided that only 3 endpoints and data persistence need to be operational
for the upcoming demo.

### The prototype

Planned home budgeting assistant is meant to operate on so-called registers. A register is conceptually similar to a
bank account - one can transfer money to and from it, and both of these operations directly affect its balance. The
system is supposed to keep track of multiple registers and allow transferring funds between them. This means an end user
can for example define registers: "wallet", "savings", "food expenses", "car maintenance" and be able to track their
spending by transferring amounts between them. In the future it would be possible to keep history of individual
transfers, archive transfers and reset balance of registers - for now, however, this is out of scope and the business
has pinpointed what they expect the app to be capable of for the sake of a successful presentation.

### Business requirements

Business deems the following aspects as must-haves:

1. A demo environment with the following registers already existing:
    1. "Wallet" register with a balance of 1000
    2. "Savings" register with a balance of 5000
    3. "Insurance policy" register with a balance of 0
    4. "Food expenses" register with a balance of 0
2. A deployed application which needs to expose the following operations:
    1. Recharge an existing register with given amount - the register's balance should then be updated accordingly with
       the requested amount added
    2. Transfer given amount between two existing registers - this means that given amount should be subtracted from
       source register's balance and added to destination register's balance
    3. Get current balance of all registers - this should simply inform about current balance of all existing registers
3. Data persistence: the application should persist balance of each register each time it is updated and even in case
   the whole system is turned off, all registers should have their balances set to previous values upon its restart.

### Technical requirements

The following guidelines have to be taken into account:

1. The prototype is expected to be built on top of Spring Boot 2 framework
2. Persistence layer should adhere to JPA specification
3. It is expected that only endpoints mentioned above will be implemented
4. Justified usage of external libraries is permitted
5. A project README file describing how the app works and how to launch it

### Example

The following scenario demonstrates what is the expected behaviour of the system. It assumes we are running a fresh
demo (with all predefined registers existing and having corresponding balances):

1. A recharge is executed for the "Wallet" register with an amount of 2500. This should increase the register's balance
   to 3500.
2. A transfer of 1500 from "Wallet" to "Food expenses" registry is executed. This should bring "Wallet" balance to 2000
   and "Food expenses" balance to 1500.
3. A transfer of 500 from "Savings" to "Insurance policy" registry is executed. This should bring "Savings" balance to
   4500 and "Insurance policy" balance to 500.
4. A transfer of 1000 from "Wallet" to "Savings" registry is executed. This should bring "Wallet" balance to 1000 and
   "Savings" balance to 5500.
5. Balance info on all registries is executed: this should print the list of all registries accompanied by their
   balance, for example:
   ```text
   Wallet: 1000
   Savings: 5500
   Insurance policy: 500
   Food expenses: 1500
   ```
