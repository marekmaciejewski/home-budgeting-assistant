package pl.mm.homebudget.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeCapabilityServiceTest {

    @Test
    void resolvesH2MemoryUrlAsEphemeral() {
        assertThat(RuntimeCapabilityService.isEphemeral("r2dbc:h2:mem:///other-demo"))
                .isTrue();
    }

    @Test
    void resolvesH2FileUrlAsPersistent() {
        assertThat(RuntimeCapabilityService.isEphemeral(
                "r2dbc:h2:file:///C:/data/home-budget"))
                .isFalse();
    }

    @Test
    void resolvesOtherDriversConservativelyAsPersistent() {
        assertThat(RuntimeCapabilityService.isEphemeral(
                "r2dbc:postgresql://localhost/home-budget"))
                .isFalse();
    }
}
