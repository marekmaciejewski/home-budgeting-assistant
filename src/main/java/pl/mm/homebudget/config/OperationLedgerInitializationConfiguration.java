package pl.mm.homebudget.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import pl.mm.homebudget.application.OperationLedgerBackfillService;

@Configuration
public class OperationLedgerInitializationConfiguration {

    @Bean
    @DependsOn("liquibase")
    SmartInitializingSingleton operationLedgerBackfillInitializer(
            OperationLedgerBackfillService operationLedgerBackfillService) {
        return () -> operationLedgerBackfillService.backfillIfNeeded().then().block();
    }
}
