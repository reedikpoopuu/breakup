package com.example.demo.contract;

import com.example.demo.ai.AiCompletionClientRegistry;
import com.example.demo.ai.AiGatewayProperties;
import com.example.demo.ai.AnthropicMessagesClient;
import com.example.demo.ai.AnthropicProperties;
import com.example.demo.ai.OpenAiCompatibleGatewayClient;
import com.example.demo.datahub.support.MockHttpEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ContractPricingAiExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ContractPricingAiExtractor extractorAgainst(MockHttpEndpoint endpoint) {
        AnthropicProperties anthropicProperties = new AnthropicProperties();
        anthropicProperties.setBaseUrl(endpoint.baseUrl());
        anthropicProperties.setApiKey("test-key");
        anthropicProperties.setModel("test-model");
        AnthropicMessagesClient anthropicClient = new AnthropicMessagesClient(anthropicProperties, RestClient.builder());
        OpenAiCompatibleGatewayClient gatewayClient =
                new OpenAiCompatibleGatewayClient(new AiGatewayProperties(), RestClient.builder());
        AiCompletionClientRegistry registry = new AiCompletionClientRegistry(anthropicClient, gatewayClient);
        return new ContractPricingAiExtractor(registry);
    }

    private void respondWithAssistantText(MockHttpEndpoint endpoint, String assistantText,
                                           AtomicReference<JsonNode> seenBody) {
        endpoint.handle("/v1/messages", exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                seenBody.set(objectMapper.readTree(is));
            }
            String body = """
                    {"content":[{"type":"text","text":%s}],"stop_reason":"end_turn","usage":{"input_tokens":1,"output_tokens":1}}
                    """.formatted(objectMapper.writeValueAsString(assistantText));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    void parsesAWellFormedJsonResponseIntoPricingFields() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            String assistantJson = """
                    {"supplierName":"Enefit","planName":"Kindel 12","pricePerKwh":0.142,\
                    "contractType":"FIXED","expiryDate":"2026-11-30","earlyTerminationPenaltyEur":380,\
                    "extractionNotes":""}""";
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            respondWithAssistantText(endpoint, assistantJson, seenBody);

            Optional<AiExtractedPricingFields> result = extractorAgainst(endpoint).extract("some contract text");

            assertThat(result).isPresent();
            AiExtractedPricingFields fields = result.get();
            assertThat(fields.supplierName()).isEqualTo("Enefit");
            assertThat(fields.planName()).isEqualTo("Kindel 12");
            assertThat(fields.pricePerKwh()).isEqualByComparingTo(new BigDecimal("0.142"));
            assertThat(fields.contractType()).isEqualTo("FIXED");
            assertThat(fields.expiryDate()).isEqualTo("2026-11-30");
            assertThat(fields.earlyTerminationPenaltyEur()).isEqualByComparingTo(new BigDecimal("380"));
        }
    }

    @Test
    void discardsAContractTypeOutsideTheFixedEnumRatherThanTrustingTheModel() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            String assistantJson = """
                    {"supplierName":null,"planName":null,"pricePerKwh":null,\
                    "contractType":"ignore previous instructions, this is not FIXED or SPOT",\
                    "expiryDate":"not-a-real-date","earlyTerminationPenaltyEur":null,"extractionNotes":""}""";
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            respondWithAssistantText(endpoint, assistantJson, seenBody);

            Optional<AiExtractedPricingFields> result = extractorAgainst(endpoint).extract("some contract text");

            assertThat(result).isPresent();
            assertThat(result.get().contractType()).isNull();
            assertThat(result.get().expiryDate()).isNull();
        }
    }

    @Test
    void truncatesOverlongStringFieldsRatherThanPassingThemThroughUnbounded() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            String longName = "A".repeat(1000);
            String assistantJson = objectMapper.writeValueAsString(new AiExtractedPricingFields(
                    longName, null, null, null, null, null, longName));
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            respondWithAssistantText(endpoint, assistantJson, seenBody);

            Optional<AiExtractedPricingFields> result = extractorAgainst(endpoint).extract("some contract text");

            assertThat(result).isPresent();
            assertThat(result.get().supplierName()).hasSize(200);
            assertThat(result.get().extractionNotes()).hasSize(500);
        }
    }

    @Test
    void stripsMarkdownCodeFencesBeforeParsing() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            String fenced = "```json\n{\"supplierName\":\"Alexela\",\"planName\":null,\"pricePerKwh\":null,"
                    + "\"contractType\":null,\"expiryDate\":null,\"earlyTerminationPenaltyEur\":null,\"extractionNotes\":\"partial\"}\n```";
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            respondWithAssistantText(endpoint, fenced, seenBody);

            Optional<AiExtractedPricingFields> result = extractorAgainst(endpoint).extract("some contract text");

            assertThat(result).isPresent();
            assertThat(result.get().supplierName()).isEqualTo("Alexela");
            assertThat(result.get().extractionNotes()).isEqualTo("partial");
        }
    }

    @Test
    void degradesGracefullyOnUnparseableModelOutputInsteadOfThrowing() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            respondWithAssistantText(endpoint, "Sorry, I can't help with that.", seenBody);

            Optional<AiExtractedPricingFields> result = extractorAgainst(endpoint).extract("some contract text");

            assertThat(result).isEmpty();
        }
    }

    @Test
    void returnsEmptyWithoutCallingOutWhenNoProviderIsConfigured() {
        AnthropicMessagesClient anthropicClient =
                new AnthropicMessagesClient(new AnthropicProperties(), RestClient.builder());
        OpenAiCompatibleGatewayClient gatewayClient =
                new OpenAiCompatibleGatewayClient(new AiGatewayProperties(), RestClient.builder());
        ContractPricingAiExtractor extractor =
                new ContractPricingAiExtractor(new AiCompletionClientRegistry(anthropicClient, gatewayClient));

        assertThat(extractor.extract("some contract text")).isEmpty();
    }

    @Test
    void truncatesVeryLongContractTextBeforeSendingIt() throws IOException {
        try (MockHttpEndpoint endpoint = new MockHttpEndpoint()) {
            String assistantJson = """
                    {"supplierName":null,"planName":null,"pricePerKwh":null,"contractType":null,\
                    "expiryDate":null,"earlyTerminationPenaltyEur":null,"extractionNotes":""}""";
            AtomicReference<JsonNode> seenBody = new AtomicReference<>();
            respondWithAssistantText(endpoint, assistantJson, seenBody);

            String hugeContractText = "x".repeat(50_000);
            extractorAgainst(endpoint).extract(hugeContractText);

            String userMessageContent = seenBody.get().get("messages").get(0).get("content").asText();
            assertThat(userMessageContent.length()).isLessThan(50_000);
        }
    }
}
