package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.RegisterConverter;
import pl.mm.homebudget.persistence.RegisterRepository;
import pl.mm.homebudget.persistence.entity.Register;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

@Service
@Profile("demo")
@RequiredArgsConstructor
public class DemoResetService {

    private final RegisterRepository registerRepository;
    private final OperationRepository operationRepository;
    private final RegisterConverter converter;
    private final R2dbcEntityTemplate entityTemplate;

    @Transactional
    public Flux<RegisterResponse> resetDemo() {
        return operationRepository.deleteAll()
                .then(registerRepository.deleteAll())
                .thenMany(demoRegisters())
                .flatMapSequential(entityTemplate::insert)
                .map(converter::toResponse);
    }

    private static Flux<Register> demoRegisters() {
        return Flux.just(
                demoRegister("Wallet", new BigDecimal("1000.00")),
                demoRegister("Savings", new BigDecimal("5000.00")),
                demoRegister("Insurance policy", new BigDecimal("0.00")),
                demoRegister("Food expenses", new BigDecimal("0.00")));
    }

    private static Register demoRegister(String id, BigDecimal balance) {
        Register register = new Register();
        register.setId(id);
        register.setBalance(balance);
        register.setActive(true);
        return register;
    }
}
