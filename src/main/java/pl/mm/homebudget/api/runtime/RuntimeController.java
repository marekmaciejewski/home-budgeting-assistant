package pl.mm.homebudget.api.runtime;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.mm.homebudget.api.RuntimeApi;
import pl.mm.homebudget.api.dto.RuntimeCapabilitiesResponse;
import pl.mm.homebudget.application.RuntimeCapabilityService;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class RuntimeController implements RuntimeApi {

    private final RuntimeCapabilityService runtimeCapabilityService;

    @Override
    public Mono<RuntimeCapabilitiesResponse> getRuntimeCapabilities(ServerWebExchange exchange) {
        return Mono.fromSupplier(runtimeCapabilityService::getCapabilities);
    }
}
