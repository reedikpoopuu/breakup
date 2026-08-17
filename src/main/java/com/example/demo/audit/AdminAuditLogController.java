package com.example.demo.audit;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Admin-only (see SecurityConfig's {@code /api/admin/**} rule) read access to the audit
 * trail - a listing endpoint for the admin UI, and a CSV export endpoint for handing to
 * an authority as proof of a customer's actions.
 */
@RestController
@RequestMapping("/api/admin/audit-log")
public class AdminAuditLogController {

    /** Leading UTF-8 BOM so Excel opens Baltic diacritics correctly instead of guessing the wrong encoding. */
    private static final String UTF8_BOM = "\uFEFF";

    private final AuditLogRepository repository;

    public AdminAuditLogController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AuditLogEntryResponse> list() {
        return repository.findAllByOrderByOccurredAtDesc().stream()
                .map(AuditLogEntryResponse::from)
                .toList();
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv() {
        List<AuditLogEntry> entries = repository.findAllByOrderByOccurredAtDesc();
        byte[] body = (UTF8_BOM + toCsv(entries)).getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("audit-log-export.csv").build());
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private static String toCsv(List<AuditLogEntry> entries) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        StringBuilder sb = new StringBuilder();
        sb.append("id,occurredAt,actionType,actorSmartIdIdentity,actorDisplayName,country,requestDetail,successful,errorMessage,responseSummary\n");
        for (AuditLogEntry e : entries) {
            sb.append(csvField(e.getId()))
                    .append(',').append(csvField(formatter.format(e.getOccurredAt())))
                    .append(',').append(csvField(e.getActionType()))
                    .append(',').append(csvField(e.getActorSmartIdIdentity()))
                    .append(',').append(csvField(e.getActorDisplayName()))
                    .append(',').append(csvField(e.getCountry()))
                    .append(',').append(csvField(e.getRequestDetail()))
                    .append(',').append(csvField(e.isSuccessful()))
                    .append(',').append(csvField(e.getErrorMessage()))
                    .append(',').append(csvField(e.getResponseSummary()))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String csvField(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
