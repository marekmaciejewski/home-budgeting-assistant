package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcProperties;
import org.springframework.stereotype.Service;
import pl.mm.homebudget.api.dto.RuntimeCapabilitiesResponse;
import pl.mm.homebudget.api.dto.RuntimeStorageMode;

@Service
@RequiredArgsConstructor
public class RuntimeCapabilityService {

    public static final String EPHEMERAL_R2DBC_URL_PREFIX = "r2dbc:h2:mem";

    @Value("${app.features.ledger-tamper-simulation-enabled:true}")
    private final boolean ledgerTamperSimulationEnabled;
    private final R2dbcProperties r2dbcProperties;

    public RuntimeCapabilitiesResponse getCapabilities() {
        return new RuntimeCapabilitiesResponse(
                resolveStorageMode(),
                ledgerTamperSimulationEnabled);
    }

    public boolean isEphemeral() {
        return resolveStorageMode() == RuntimeStorageMode.EPHEMERAL;
    }

    private RuntimeStorageMode resolveStorageMode() {
        return resolveStorageMode(r2dbcProperties.getUrl());
    }

    public static RuntimeStorageMode resolveStorageMode(String r2dbcUrl) {
        return r2dbcUrl.startsWith(EPHEMERAL_R2DBC_URL_PREFIX)
                ? RuntimeStorageMode.EPHEMERAL
                : RuntimeStorageMode.PERSISTENT;
    }
}
