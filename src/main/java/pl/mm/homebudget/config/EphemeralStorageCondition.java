package pl.mm.homebudget.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import pl.mm.homebudget.api.dto.RuntimeStorageMode;
import pl.mm.homebudget.application.RuntimeCapabilityService;

public class EphemeralStorageCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String r2dbcUrl = context.getEnvironment().getProperty("spring.r2dbc.url");
        return r2dbcUrl != null
                && RuntimeCapabilityService.resolveStorageMode(r2dbcUrl) == RuntimeStorageMode.EPHEMERAL;
    }
}
