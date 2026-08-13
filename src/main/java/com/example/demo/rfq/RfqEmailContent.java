package com.example.demo.rfq;

/**
 * A generated request-for-quote email, ready to hand to a mail sender. {@code from} is
 * the per-company Google Workspace alias (e.g. {@code nordicwoods@quote.easybreak.com})
 * so supplier replies thread back to the right company (ARCH_SPEC.md section 1.4).
 */
public record RfqEmailContent(String from, String to, String subject, String body) {
}
