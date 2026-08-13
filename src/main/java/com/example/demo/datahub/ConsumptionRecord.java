package com.example.demo.datahub;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One metering interval pulled from a national DataHub. Stored at the granularity the
 * adapter provides (hourly where available, e.g. Estfeed) rather than collapsed to
 * monthly totals, so a later spot-cost model is not blocked (ARCH_SPEC.md section 3.3).
 * {@code companyId} is a plain FK - the Company entity and the eicCode-to-company
 * resolution that fills it in live in the not-yet-built async fetch job (ARCH_SPEC.md
 * section 3.5 / build order step 4), so {@link DataHubClient} adapters construct these
 * with {@code companyId == null} and the job assigns it before persisting.
 */
@Entity
@Table(name = "consumption_records")
public class ConsumptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "interval_start", nullable = false)
    private Instant intervalStart;

    @Column(name = "interval_end", nullable = false)
    private Instant intervalEnd;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal kwh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Granularity granularity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataHubSource source;

    protected ConsumptionRecord() {
        // JPA
    }

    public ConsumptionRecord(Long companyId, Instant intervalStart, Instant intervalEnd, BigDecimal kwh,
                              Granularity granularity, DataHubSource source) {
        this.companyId = companyId;
        this.intervalStart = intervalStart;
        this.intervalEnd = intervalEnd;
        this.kwh = kwh;
        this.granularity = granularity;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Instant getIntervalStart() {
        return intervalStart;
    }

    public Instant getIntervalEnd() {
        return intervalEnd;
    }

    public BigDecimal getKwh() {
        return kwh;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public DataHubSource getSource() {
        return source;
    }
}
