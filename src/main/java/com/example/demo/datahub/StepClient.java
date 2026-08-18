package com.example.demo.datahub;

import com.example.demo.common.CountryCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * STEP / Sadales tīkls (LV) adapter, rebuilt 2026-08 against the real spec: "Data
 * Platform STEP - Single Data Exchange Standard" v1.6
 * (sadalestikls.lv/storage/app/media/platforma/Single_Data_Exchange_Standard_v1.6_ENG.pdf).
 * Two very different confidence levels live in this class:
 * <p>
 * <b>GetObjectConsumption (confirmed):</b> the request/response element names,
 * types, and cardinality below are transcribed directly from the spec's Tables 13-14
 * ("GetObjectConsumption request/reply structure"): request {@code
 * MMIdentification/messageIc}, {@code customerEIC}, {@code objectEIC}, {@code
 * customerPermission}, {@code dateFrom}/{@code dateTo}, {@code registerType}; reply
 * {@code customerEIC}/{@code objectEIC}/{@code registerType}/{@code
 * averageYearlyConsumption}/repeating {@code consInfo[consDT,cons]}. {@code
 * registerType} is fixed to "A+" (consumption) here - the spec's other value, "A-",
 * is production/export data this app never needs. The spec states each interval is
 * "1h or 15 minutes" without saying which applies to a given reading, so the interval
 * end computed below assumes hourly - unconfirmed, and wrong for any customer STEP
 * actually meters at 15-minute resolution.
 * <p>
 * <b>Auth handshake (best-effort, NOT confirmed):</b> the spec describes Bearer JWT
 * auth only conceptually - a "system:system user" account is created, a one-time
 * token arrives by e-mail and is used exactly once against a {@code ChangeCredentials}
 * service to set a password (a one-time human step this class does not attempt), and
 * the resulting username/password is then exchanged for a JWT at a SOAP "Auth
 * service" - but the actual login request/response is shown only as SoapUI
 * screenshots in the published PDF, with no extractable text. {@link #login()} is
 * therefore a structurally-reasonable guess (SOAP call, username/password in, a JWT
 * string out), not a verified implementation - the same confidence level this whole
 * class carried before this rebuild, now scoped down to just this one method. Fix
 * once the real WSDL is issued after registering as a market participant (see the
 * spec's "Admission of new participants" section - there is no public sandbox).
 */
@Component
public class StepClient implements DataHubClient {

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    // Not confirmed - the real GetObjectConsumption XSD targetNamespace ships only as an
    // external attachment (STDHMMElementTypes.xsd etc.), never as text in the spec PDF.
    private static final String STDH_NS = "http://step.sadalestikls.lv/stdh";
    private static final MediaType TEXT_XML = MediaType.valueOf("text/xml; charset=UTF-8");
    private static final DateTimeFormatter STDH_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StepProperties properties;
    private final RestClient.Builder restClientBuilder;

    public StepClient(StepProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public CountryCode getCountry() {
        return CountryCode.LV;
    }

    @Override
    public List<ConsumptionRecord> fetchConsumption(String customerEic, String objectEic, boolean customerPermission,
                                                      LocalDate from, LocalDate to) {
        if (!properties.isConfigured()) {
            throw new DataHubNotConfiguredException(CountryCode.LV);
        }
        String jwt = login();
        String requestXml = buildGetObjectConsumptionRequest(customerEic, objectEic, customerPermission, from, to);

        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        String responseXml = restClient.post()
                .uri("/MarketMessagesSupplier")
                .header("Authorization", "Bearer " + jwt)
                .header("SOAPAction", "GetObjectConsumption")
                .contentType(TEXT_XML)
                .body(requestXml)
                .retrieve()
                .body(String.class);

        return parseGetObjectConsumptionResponse(responseXml);
    }

    // ---- GetObjectConsumption: confirmed against Tables 13-14 of the real spec ----

    static String buildGetObjectConsumptionRequest(String customerEic, String objectEic, boolean customerPermission,
                                                     LocalDate from, LocalDate to) {
        try {
            Document doc = newDocument();
            Element envelope = doc.createElementNS(SOAP_NS, "soap:Envelope");
            doc.appendChild(envelope);
            Element body = doc.createElementNS(SOAP_NS, "soap:Body");
            envelope.appendChild(body);

            Element request = doc.createElementNS(STDH_NS, "GetObjectConsumptionRequest");
            body.appendChild(request);

            Element mmId = doc.createElementNS(STDH_NS, "MMIdentification");
            request.appendChild(mmId);
            appendText(doc, mmId, "messageIc", UUID.randomUUID().toString());

            appendText(doc, request, "customerEIC", customerEic);
            appendText(doc, request, "objectEIC", objectEic);
            appendText(doc, request, "customerPermission", String.valueOf(customerPermission));
            if (from != null) {
                appendText(doc, request, "dateFrom", from.format(STDH_DATE));
            }
            if (to != null) {
                appendText(doc, request, "dateTo", to.format(STDH_DATE));
            }
            appendText(doc, request, "registerType", "A+");

            return serialize(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build GetObjectConsumption request", e);
        }
    }

    static List<ConsumptionRecord> parseGetObjectConsumptionResponse(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        try {
            Document doc = newDocumentBuilderFactory().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            NodeList consInfos = doc.getElementsByTagNameNS("*", "consInfo");
            List<ConsumptionRecord> records = new ArrayList<>();
            for (int i = 0; i < consInfos.getLength(); i++) {
                Element consInfo = (Element) consInfos.item(i);
                String consDt = textOf(consInfo, "consDT");
                String cons = textOf(consInfo, "cons");
                if (consDt == null || cons == null) {
                    continue;
                }
                Instant start = Instant.parse(consDt);
                records.add(new ConsumptionRecord(
                        null,
                        start,
                        start.plus(Duration.ofHours(1)),
                        new BigDecimal(cons),
                        Granularity.HOURLY,
                        DataHubSource.STEP));
            }
            return records;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse GetObjectConsumption response", e);
        }
    }

    // ---- Auth handshake: best-effort, see class javadoc ----

    private String login() {
        RestClient authClient = restClientBuilder.baseUrl(properties.getAuthBaseUrl()).build();
        String requestXml = buildLoginRequest(properties.getUsername(), properties.getPassword());
        String responseXml = authClient.post()
                .uri("/Auth")
                .header("SOAPAction", "Login")
                .contentType(TEXT_XML)
                .body(requestXml)
                .retrieve()
                .body(String.class);
        String jwt = extractLoginToken(responseXml);
        if (jwt == null || jwt.isBlank()) {
            throw new DataHubNotConfiguredException(CountryCode.LV);
        }
        return jwt;
    }

    static String buildLoginRequest(String username, String password) {
        try {
            Document doc = newDocument();
            Element envelope = doc.createElementNS(SOAP_NS, "soap:Envelope");
            doc.appendChild(envelope);
            Element body = doc.createElementNS(SOAP_NS, "soap:Body");
            envelope.appendChild(body);
            Element login = doc.createElementNS(STDH_NS, "Login");
            body.appendChild(login);
            appendText(doc, login, "username", username);
            appendText(doc, login, "password", password);
            return serialize(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build login request", e);
        }
    }

    static String extractLoginToken(String xml) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        try {
            Document doc = newDocumentBuilderFactory().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            NodeList tokens = doc.getElementsByTagNameNS("*", "token");
            if (tokens.getLength() == 0) {
                tokens = doc.getElementsByTagNameNS("*", "jwt");
            }
            return tokens.getLength() > 0 ? tokens.item(0).getTextContent() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- shared XML helpers ----

    private static Document newDocument() throws Exception {
        return newDocumentBuilderFactory().newDocumentBuilder().newDocument();
    }

    // Namespace-aware is required for getElementsByTagNameNS (used when parsing responses) to
    // match anything at all - DocumentBuilderFactory defaults to namespace-UNaware, under which
    // getElementsByTagNameNS silently returns zero results regardless of the wildcard used.
    //
    // This same factory also parses inbound XML from the external STEP DataHub
    // (parseGetObjectConsumptionResponse, extractLoginToken) - a compromised, spoofed, or
    // MITM'd response could otherwise carry an XXE payload (local file disclosure, internal
    // SSRF via an entity URL, entity-expansion DoS), since Java resolves external entities
    // and processes DOCTYPEs by default. SOAP responses from STEP have no legitimate use for
    // a DOCTYPE, so disallowing it outright costs nothing functionally.
    private static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory;
    }

    private static void appendText(Document doc, Element parent, String localName, String value) {
        Element el = doc.createElementNS(STDH_NS, localName);
        el.setTextContent(value);
        parent.appendChild(el);
    }

    private static String textOf(Element parent, String localName) {
        NodeList children = parent.getElementsByTagNameNS("*", localName);
        return children.getLength() > 0 ? children.item(0).getTextContent() : null;
    }

    private static String serialize(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
