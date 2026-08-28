package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcProperties;
import org.springframework.stereotype.Service;
import pl.mm.homebudget.api.dto.RuntimeCapabilitiesResponse;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RuntimeCapabilityService {

    public static final String EPHEMERAL_R2DBC_URL_PREFIX = "r2dbc:h2:mem";

    @Value("${app.features.ledger-tamper-simulation-enabled:true}")
    private final boolean ledgerTamperSimulationEnabled;
    private final R2dbcProperties r2dbcProperties;

    public RuntimeCapabilitiesResponse getCapabilities() {
        return new RuntimeCapabilitiesResponse(
                isEphemeral(),
                ledgerTamperSimulationEnabled);
    }

    public boolean isEphemeral() {
        return isEphemeral(r2dbcProperties.getUrl());
    }

    public static boolean isEphemeral(String r2dbcUrl) {
        return Optional.ofNullable(r2dbcUrl)
                .filter(url -> url.startsWith(EPHEMERAL_R2DBC_URL_PREFIX))
                .isPresent();
    }
}
