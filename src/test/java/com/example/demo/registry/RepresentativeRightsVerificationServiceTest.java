package com.example.demo.registry;

import com.example.demo.common.CountryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepresentativeRightsVerificationServiceTest {

    private static final CompanyRepresentative BOARD_MEMBER = new CompanyRepresentative(
            "Jaan", "Tamm", "38001085718", "EST", null, "BOARD", "Board member", true);

    private RepresentativeRightsVerificationService service;

    private RepresentationRightsClient stubClient(List<CompanyRepresentative> representatives) {
        return new RepresentationRightsClient() {
            @Override
            public CountryCode getCountry() {
                return CountryCode.EE;
            }

            @Override
            public List<CompanyRepresentative> fetchRepresentatives(String registryCode) {
                return representatives;
            }
        };
    }

    @BeforeEach
    void setUp() {
        RepresentationRightsClientResolver resolver = new RepresentationRightsClientResolver(
                List.of(stubClient(List.of(BOARD_MEMBER))));
        service = new RepresentativeRightsVerificationService(resolver);
    }

    @Test
    void verifiesWhenTheLoginPersonalIdCodeAndCountryMatchARepresentative() {
        RepresentationRightsVerification result = service.verify(CountryCode.EE, "12345678", "EE-38001085718");

        assertThat(result.verified()).isTrue();
        assertThat(result.matchedRole()).isEqualTo("BOARD");
        assertThat(result.exclusiveRightOfRepresentation()).isTrue();
        assertThat(result.personalIdCode()).isEqualTo("38001085718");
        assertThat(result.personalIdCountry()).isEqualTo("EST");
    }

    @Test
    void doesNotVerifyWhenThePersonalIdCodeIsNotInTheList() {
        RepresentationRightsVerification result = service.verify(CountryCode.EE, "12345678", "EE-99999999999");

        assertThat(result.verified()).isFalse();
        assertThat(result.matchedRole()).isNull();
    }

    @Test
    void doesNotVerifyWhenTheCodeMatchesButTheCountryDoesNot() {
        // Same national ID number, different issuing country - must not match since a
        // personal ID number is only unique within its own country.
        RepresentationRightsClientResolver resolver = new RepresentationRightsClientResolver(
                List.of(stubClient(List.of(BOARD_MEMBER))));
        RepresentativeRightsVerificationService serviceWithForeignLogin = new RepresentativeRightsVerificationService(resolver);

        RepresentationRightsVerification result = serviceWithForeignLogin.verify(CountryCode.EE, "12345678", "LV-38001085718");

        assertThat(result.verified()).isFalse();
    }

    @Test
    void rejectsASmartIdIdentityNotInCountryDashCodeForm() {
        assertThatThrownBy(() -> service.verify(CountryCode.EE, "12345678", "notavalididentity"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
