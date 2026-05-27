package pl.mm.homebudget.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RegisterConfigurationTest {

    @Test
    void clock_returnsNotNullObject() {
        assertNotNull(new RegisterConfiguration().clock());
    }
}
