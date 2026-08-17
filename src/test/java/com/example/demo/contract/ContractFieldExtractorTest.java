package com.example.demo.contract;

import com.example.demo.common.CountryCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractFieldExtractorTest {

    private final ContractFieldExtractor extractor = new ContractFieldExtractor();

    @Test
    void extractsAnEicCode() {
        ExtractedContractFields result = extractor.extract("Objekti EIC kood: 38ZEE-1000009--Z, muud andmed...");

        assertThat(result.eicCodes()).containsExactly("38ZEE-1000009--Z");
    }

    @Test
    void deduplicatesRepeatedEicCodes() {
        ExtractedContractFields result = extractor.extract("38ZEE-1000009--Z appears twice: 38ZEE-1000009--Z");

        assertThat(result.eicCodes()).containsExactly("38ZEE-1000009--Z");
    }

    @Test
    void ignoresTextThatDoesNotMatchTheEicShape() {
        ExtractedContractFields result = extractor.extract("Invoice number: 1234567890123456, phone: 55512345678");

        assertThat(result.eicCodes()).isEmpty();
    }

    @Test
    void extractsAnEstonianRegistryCodeNearItsLabel() {
        ExtractedContractFields result = extractor.extract("Müüja: Enefit AS, Registrikood: 10421629, aadress...");

        assertThat(result.registryCodeCandidates())
                .containsExactly(new RegistryCodeCandidate(CountryCode.EE, "10421629"));
    }

    @Test
    void extractsALatvianRegistryCodeNearItsLabel() {
        ExtractedContractFields result = extractor.extract("Pārdevējs: SIA Piemērs, Reģistrācijas numurs: 40103123456.");

        assertThat(result.registryCodeCandidates())
                .containsExactly(new RegistryCodeCandidate(CountryCode.LV, "40103123456"));
    }

    @Test
    void extractsALithuanianRegistryCodeNearItsLabel() {
        ExtractedContractFields result = extractor.extract("Pardavėjas: UAB Pavyzdys, Įmonės kodas: 302123456, adresas...");

        assertThat(result.registryCodeCandidates())
                .containsExactly(new RegistryCodeCandidate(CountryCode.LT, "302123456"));
    }

    @Test
    void doesNotMatchDigitsWithNoRegistryLabelNearby() {
        ExtractedContractFields result = extractor.extract("Client phone: 12345678, order total: 40103123456 cents.");

        assertThat(result.registryCodeCandidates()).isEmpty();
    }

    @Test
    void handlesARealisticMultiFieldContractExcerpt() {
        String text = """
                ELEKTRIENERGIA MÜÜGILEPING
                Müüja: Enefit AS, Registrikood: 10421629
                Ostja objekti EIC kood: 38ZEE-1000009--Z
                Paketi hind: 12,67 senti/kWh
                """;

        ExtractedContractFields result = extractor.extract(text);

        assertThat(result.eicCodes()).containsExactly("38ZEE-1000009--Z");
        assertThat(result.registryCodeCandidates())
                .containsExactly(new RegistryCodeCandidate(CountryCode.EE, "10421629"));
    }
}
