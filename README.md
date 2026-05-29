# home-budgeting-assistant

## Preparation

Before the application can be started, an H2 database file path must be provided in `application.yml` for both the R2DBC
application connection and the JDBC Liquibase migration connection. The URLs should point at the same database file:

```yaml
spring:
  r2dbc:
    url: "r2dbc:h2:file:///C:/Users/you/foo/bar/db/registers;DB_CLOSE_ON_EXIT=FALSE"
  liquibase:
    url: "jdbc:h2:file:C:/Users/you/foo/bar/db/registers;DB_CLOSE_ON_EXIT=FALSE"
```

## Instruction

The app can be run from IDE or maven plugin: `.\mvnw.cmd spring-boot:run`.
The DB will be loaded with initial state of following registers:

| register         | balance |
|------------------|---------|
| Wallet           | 1000    |
| Savings          | 5000    |
| Insurance policy | 0       |
| Food expenses    | 0       |

To start from beginning simply stop the app and delete DB file in the provided earlier directory.

## Interact

The app exposes the following primary API on the default host:

| method | url                                  | description                         |
|--------|--------------------------------------|-------------------------------------|
| GET    | `/registers`                         | get all active registers            |
| GET    | `/registers/{registerId}`            | get one active register             |
| POST   | `/registers/{registerId}/recharges`  | recharge a register                 |
| POST   | `/transfers`                         | transfer between registers          |
| GET    | `/operations`                        | get all balance-changing operations |
| GET    | `/operations/{operationId}`          | get one balance-changing operation  |

Register IDs are currently register names, so names containing spaces must be URL-encoded in path variables.

The details of the input/output model are available on the swagger ui url:
`localhost:8080/swagger-ui.html`
This is also the easiest way to interact with the app.

The checked-in OpenAPI contract lives in `src/main/resources/openapi/home-budget-api.yaml`.
Maven generates the Spring WebFlux API interfaces and request/response DTOs from that file during the build.

## Sample requests

### POST /registers/Wallet/recharges

```json
{
  "amount": 2500
}
```

### POST /transfers

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
