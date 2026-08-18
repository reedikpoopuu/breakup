package com.example.demo.registry;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Selects the right {@link RepresentationRightsClient} for a company's country at call time. */
@Component
public class RepresentationRightsClientResolver {

    private final Map<CountryCode, RepresentationRightsClient> clientsByCountry;

    public RepresentationRightsClientResolver(List<RepresentationRightsClient> clients) {
        this.clientsByCountry = clients.stream()
                .collect(Collectors.toUnmodifiableMap(RepresentationRightsClient::getCountry, Function.identity()));
    }

    public RepresentationRightsClient resolve(CountryCode country) {
        RepresentationRightsClient client = clientsByCountry.get(country);
        if (client == null) {
            throw new IllegalArgumentException("No business registry adapter registered for " + country);
        }
        return client;
    }
}
