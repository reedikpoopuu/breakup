package com.example.demo.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundUrlValidatorTest {

    private final OutboundUrlValidator validator = new OutboundUrlValidator();

    @Test
    void rejectsTheCloudMetadataAddress() {
        assertThatThrownBy(() -> validator.validate("http://169.254.169.254/latest/meta-data/iam/security-credentials/"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
    }

    @Test
    void rejectsLoopback() {
        assertThatThrownBy(() -> validator.validate("http://127.0.0.1/admin"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
    }

    @Test
    void rejectsRfc1918PrivateRanges() {
        assertThatThrownBy(() -> validator.validate("http://10.0.0.5/"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
        assertThatThrownBy(() -> validator.validate("http://192.168.1.1/"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
        assertThatThrownBy(() -> validator.validate("http://172.16.0.1/"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> validator.validate("file:///etc/passwd"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
        assertThatThrownBy(() -> validator.validate("ftp://example.com/x"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
    }

    @Test
    void rejectsMalformedUrls() {
        assertThatThrownBy(() -> validator.validate("not a url"))
                .isInstanceOf(UnsafeOutboundUrlException.class);
    }

    @Test
    void allowsAPublicIpLiteral() {
        assertThatCode(() -> validator.validate("http://93.184.216.34/price"))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsAnOrdinaryHostname() {
        assertThatCode(() -> validator.validate("https://alexela.ee/hinnad"))
                .doesNotThrowAnyException();
    }
}
