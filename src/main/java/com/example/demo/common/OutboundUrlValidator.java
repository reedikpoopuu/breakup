package com.example.demo.common;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Guards any URL this app fetches server-side on someone's behalf (today: admin-set
 * supplier price pages, {@code RestClientSupplierPageFetcher}) against SSRF - an admin
 * (or anyone holding a leaked/un-revocable admin token) setting the URL to a cloud
 * metadata endpoint or an internal-only service, and reading the response back through
 * the admin API. Called both at write time ({@code SupplierService}, for immediate
 * feedback) and again right before each fetch ({@code RestClientSupplierPageFetcher}).
 * <p>
 * Scope, stated honestly: this only rejects URLs whose host is already a literal IP
 * address in a private/loopback/link-local range - which is exactly what the classic
 * cloud-metadata payload (e.g. {@code http://169.254.169.254/latest/meta-data/...}) and
 * a direct {@code http://127.0.0.1/...} target look like, and needs no DNS lookup to
 * catch. It deliberately does NOT resolve hostname-form URLs (e.g. {@code
 * https://example.com}) to check where they point - that would mean a blocking DNS call
 * on every check (fragile in tests, adds latency, and is itself vulnerable to DNS
 * rebinding between check and connect). A hostname whose DNS record points at an
 * internal address is not caught by this check alone; closing that gap fully needs
 * resolution tied to the actual connection (a custom resolver on the HTTP client), not a
 * separate validation step - out of scope for this pass.
 */
@Component
public class OutboundUrlValidator {

    private static final Pattern IPV4_LITERAL = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");

    public void validate(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new UnsafeOutboundUrlException(url, "not a valid URL");
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new UnsafeOutboundUrlException(url, "scheme must be http or https");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeOutboundUrlException(url, "missing host");
        }
        if (!isIpLiteral(host)) {
            return;
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new UnsafeOutboundUrlException(url, "not a valid IP address");
        }
        // Covers loopback (127.0.0.0/8), RFC 1918 private ranges (10.0.0.0/8,
        // 172.16.0.0/12, 192.168.0.0/16), link-local (169.254.0.0/16 - which is also
        // where the AWS/GCP/Azure instance metadata endpoint lives), and 0.0.0.0.
        if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
            throw new UnsafeOutboundUrlException(url, "resolves to a private or internal address");
        }
    }

    // Real hostnames per DNS syntax never contain a literal colon (a "host:port" pair is
    // already split apart by URI.getHost() before this runs) or match a dotted-quad -
    // this stays a same-thread, no-network check specifically so it never needs DNS.
    private static boolean isIpLiteral(String host) {
        return IPV4_LITERAL.matcher(host).matches() || host.contains(":");
    }
}
