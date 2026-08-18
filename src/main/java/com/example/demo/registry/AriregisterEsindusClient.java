package com.example.demo.registry;

import com.example.demo.common.CountryCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Estonian e-Business Register (Ariregister) adapter for the {@code arireg.esindus_v1}
 * "rights of representation - all persons related to a company" open-data query,
 * built 2026-08 against
 * https://avaandmed.ariregister.rik.ee/en/open-data-api/rights-representation-all-persons-related-company
 * and its linked XSD (http://www2.rik.ee/schemas/xtee6/arireg/live/xroad6_esindus_v1.xsd).
 * <p>
 * <b>Confirmed from the XSD:</b> target namespace {@code
 * http://arireg.x-road.eu/producer/}; request root element {@code esindus_v1} with
 * (among others) {@code ariregister_kasutajanimi}/{@code ariregister_parool} (an
 * open-data account username/password, not an API key or X-Road member certificate)
 * and {@code ariregistri_kood} (the company registry code to look up); response root {@code
 * esindus_v1Response} containing a {@code keha} (body) element whose {@code
 * ettevotjad} holds one {@code item} per matched company, each with an {@code isikud}
 * list of {@code item} person entries carrying {@code fyysilise_isiku_kood} +
 * {@code isikukood_riik} (the person's personal ID code and its country, ISO
 * 3166-1 alpha-3 - e.g. "EST") plus name, role, and {@code
 * ainuesindusoigus_olemas} (exclusive right of representation).
 * <p>
 * <b>Not confirmed:</b> the exact SOAP endpoint hostname/path, and the credentials
 * themselves - RIK's abiinfo.rik.ee FAQ states plainly that "API teenuste kasutamine
 * eeldab lepingu sõlmimist" (using API services requires signing a contract); the
 * open-data documentation page describes the request/response shape but does not
 * publish the endpoint or a self-service signup. See "Getting real credentials" in
 * {@code docs/api-specs/business-registry/ee.md} for the application process. {@code
 * app.registry.ariregister.base-url} must be supplied once obtained (see
 * {@link AriregisterProperties}). The optional output-format field is deliberately
 * left unset here rather than guessed at - the two sources read while building this
 * class disagreed on its exact name ({@code ariregistri_valjundi_formaat} vs. {@code
 * ariregister_valjundi_formaat}) - the query defaults to XML either way, which is what
 * this class parses, so nothing is lost by omitting it. Whether a {@code SOAPAction}
 * header is required, and any rate limits, are also unstated in the docs; a {@code
 * SOAPAction} header is sent on a best-effort basis since it is harmless if unneeded.
 */
@Component
public class AriregisterEsindusClient implements RepresentationRightsClient {

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String ARIREG_NS = "http://arireg.x-road.eu/producer/";
    private static final MediaType TEXT_XML = MediaType.valueOf("text/xml; charset=UTF-8");

    private final AriregisterProperties properties;
    private final RestClient.Builder restClientBuilder;

    public AriregisterEsindusClient(AriregisterProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public CountryCode getCountry() {
        return CountryCode.EE;
    }

    @Override
    public List<CompanyRepresentative> fetchRepresentatives(String registryCode) {
        if (!properties.isConfigured()) {
            throw new RegistryNotConfiguredException(CountryCode.EE);
        }
        String requestXml = buildEsindusRequest(properties.getUsername(), properties.getPassword(), registryCode);

        RestClient restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        String responseXml = restClient.post()
                .uri("")
                .header("SOAPAction", "esindus_v1")
                .contentType(TEXT_XML)
                .body(requestXml)
                .retrieve()
                .body(String.class);

        return parseEsindusResponse(responseXml);
    }

    // ---- request building - confirmed field names/order per the XSD ----

    static String buildEsindusRequest(String username, String password, String registryCode) {
        try {
            Document doc = newDocument();
            Element envelope = doc.createElementNS(SOAP_NS, "soap:Envelope");
            doc.appendChild(envelope);
            Element body = doc.createElementNS(SOAP_NS, "soap:Body");
            envelope.appendChild(body);

            Element request = doc.createElementNS(ARIREG_NS, "esindus_v1");
            body.appendChild(request);

            appendText(doc, request, "ariregister_kasutajanimi", username);
            appendText(doc, request, "ariregister_parool", password);
            appendText(doc, request, "ariregistri_kood", registryCode);

            return serialize(doc);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build esindus_v1 request", e);
        }
    }

    // ---- response parsing - confirmed field names/nesting per the XSD ----

    static List<CompanyRepresentative> parseEsindusResponse(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        try {
            Document doc = newDocumentBuilderFactory().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));
            Element keha = firstDescendant(doc.getDocumentElement(), "keha");
            if (keha == null) {
                return List.of();
            }
            Element ettevotjad = firstDirectChild(keha, "ettevotjad");
            if (ettevotjad == null) {
                return List.of();
            }

            List<CompanyRepresentative> representatives = new ArrayList<>();
            for (Element company : directChildren(ettevotjad, "item")) {
                Element isikud = firstDirectChild(company, "isikud");
                if (isikud == null) {
                    continue;
                }
                for (Element person : directChildren(isikud, "item")) {
                    representatives.add(toRepresentative(person));
                }
            }
            return representatives;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse esindus_v1 response", e);
        }
    }

    private static CompanyRepresentative toRepresentative(Element person) {
        String birthDateText = textOf(person, "fyysilise_isiku_synniaeg");
        LocalDate dateOfBirth = birthDateText == null || birthDateText.isBlank() ? null : LocalDate.parse(birthDateText);
        return new CompanyRepresentative(
                textOf(person, "fyysilise_isiku_eesnimi"),
                textOf(person, "fyysilise_isiku_perenimi"),
                textOf(person, "fyysilise_isiku_kood"),
                textOf(person, "isikukood_riik"),
                dateOfBirth,
                textOf(person, "fyysilise_isiku_roll"),
                textOf(person, "fyysilise_isiku_roll_tekstina"),
                "true".equalsIgnoreCase(textOf(person, "ainuesindusoigus_olemas")));
    }

    // ---- shared XML helpers (XXE-safe parsing mirrors StepClient) ----

    private static Document newDocument() throws Exception {
        return newDocumentBuilderFactory().newDocumentBuilder().newDocument();
    }

    // Namespace-aware + DOCTYPE/external-entity disabled, same rationale as StepClient: a
    // compromised, spoofed, or MITM'd registry response is untrusted input like any other,
    // and a business-registry response has no legitimate use for a DOCTYPE.
    private static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory;
    }

    private static void appendText(Document doc, Element parent, String localName, String value) {
        Element el = doc.createElementNS(ARIREG_NS, localName);
        el.setTextContent(value);
        parent.appendChild(el);
    }

    /** First matching element anywhere in the subtree, namespace-agnostic (StepClient's approach). */
    private static Element firstDescendant(Element context, String localName) {
        NodeList matches = context.getElementsByTagNameNS("*", localName);
        return matches.getLength() > 0 ? (Element) matches.item(0) : null;
    }

    /**
     * Immediate {@code item} children only - {@code getElementsByTagNameNS} returns every
     * descendant regardless of depth, which would merge company-level and person-level
     * {@code item} elements together and silently produce nonsense. Direct-child traversal
     * keeps the two nesting levels distinct.
     */
    private static List<Element> directChildren(Element parent, String localName) {
        List<Element> children = new ArrayList<>();
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && localName.equals(node.getLocalName())) {
                children.add((Element) node);
            }
        }
        return children;
    }

    private static Element firstDirectChild(Element parent, String localName) {
        List<Element> children = directChildren(parent, localName);
        return children.isEmpty() ? null : children.get(0);
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
