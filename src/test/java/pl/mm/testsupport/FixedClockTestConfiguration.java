package pl.mm.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfiguration {

    public static final Instant FIXED_INSTANT = Instant.parse("2026-06-01T10:15:30Z");

    @Bean
    @Primary
    Clock fixedClock() {
        return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    }
}
