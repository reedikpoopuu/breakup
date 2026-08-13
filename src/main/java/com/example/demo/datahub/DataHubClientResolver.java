package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Selects the right {@link DataHubClient} for a company's country at call time. */
@Component
public class DataHubClientResolver {

    private final Map<CountryCode, DataHubClient> clientsByCountry;

    public DataHubClientResolver(List<DataHubClient> clients) {
        this.clientsByCountry = clients.stream()
                .collect(Collectors.toUnmodifiableMap(DataHubClient::getCountry, Function.identity()));
    }

    public DataHubClient resolve(CountryCode country) {
        DataHubClient client = clientsByCountry.get(country);
        if (client == null) {
            throw new IllegalArgumentException("No DataHub adapter registered for " + country);
        }
        return client;
    }
}
