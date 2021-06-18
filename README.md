# home-budgeting-assistant

## Preparation
Before the application can be started a directory for an H2 database must be provided in `application.properties` so the whole url will look similar to the example below:
`spring.datasource.url=jdbc:h2:file:C:/Users/you/foo/bar/db/registers;DB_CLOSE_ON_EXIT=FALSE`

## Instruction
The app can be run from IDE or maven plugin: `mvn spring-boot:run`.
The DB will be loaded with initial state of following registers:

|register|balance|
|---|---|
|Wallet|1000|
|Savings|5000|
|Insurance policy|0|
|Food expenses|0|

To start from beginning simply stop the app and delete DB file in the provided earlier directory.

## Interact
The app exposes three endpoints on the default ip address:

|method|url|description|
|---|---|---|
|POST|`/registers/recharge`|recharge register|
|POST|`/registers/transfer`|transfer between registers|
|GET|`/registers`|balances printout|

The details of the input/output model are available on the swagger ui url:
`localhost:8080/swagger-ui.html`
This is also the easiest way to interact with the app.

## Sample requests
### /recharge
```json
{
    "registerName":"Wallet",
    "amount":2500
}
```
### /transfer
```json
{
    "sourceRegister":"Wallet",
    "targetRegister":"Food expenses",
    "amount":1500
}
```

## Tests
To run all the unit and integration tests run command: `mvn clean verify`.
