package pl.mm.homebudget.application;

import org.junit.jupiter.api.Test;
import pl.mm.homebudget.api.dto.RuntimeStorageMode;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeCapabilityServiceTest {

    @Test
    void resolvesH2MemoryUrlAsEphemeral() {
        assertThat(RuntimeCapabilityService.resolveStorageMode("r2dbc:h2:mem:///other-demo"))
                .isEqualTo(RuntimeStorageMode.EPHEMERAL);
    }

    @Test
    void resolvesH2FileUrlAsPersistent() {
        assertThat(RuntimeCapabilityService.resolveStorageMode(
                "r2dbc:h2:file:///C:/data/home-budget"))
                .isEqualTo(RuntimeStorageMode.PERSISTENT);
    }

    @Test
    void resolvesOtherDriversConservativelyAsPersistent() {
        assertThat(RuntimeCapabilityService.resolveStorageMode(
                "r2dbc:postgresql://localhost/home-budget"))
                .isEqualTo(RuntimeStorageMode.PERSISTENT);
    }
}
