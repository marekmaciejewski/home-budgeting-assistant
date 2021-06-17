package com.solera.budgeting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RegisterConfigurationTest {

    @Test
    void clock_returnsNotNullObject() {
        assertNotNull(new RegisterConfiguration().clock());
    }
}
