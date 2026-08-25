package pl.mm.homebudget.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pl.mm.homebudget.HomeBudgetApplication;

import static org.assertj.core.api.Assertions.assertThat;

class OperationLedgerStartupFailureIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(HomeBudgetApplication.class)
            .withPropertyValues(
                    "spring.r2dbc.url=r2dbc:h2:mem:///ledgerstartupfailure;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.liquibase.url=jdbc:h2:mem:ledgerstartupfailure;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.liquibase.contexts=it-ledger-mixed-state",
                    "app.cors.allowed-origins=http://localhost");

    @Test
    void startupFailsFastOnMixedLedgerState() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Operation ledger has a mixed backfill state");
        });
    }
}
