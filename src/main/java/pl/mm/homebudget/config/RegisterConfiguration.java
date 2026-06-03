package pl.mm.homebudget.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RegisterConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
