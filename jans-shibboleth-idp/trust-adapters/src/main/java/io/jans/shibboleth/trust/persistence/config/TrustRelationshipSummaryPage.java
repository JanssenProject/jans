package io.jans.shibboleth.trust.persistence.config;

import io.jans.shibboleth.trust.dto.config.TrustRelationshipSummary;

import java.util.List;

/**
 * A page of view summaries (read models, TP10) plus the total count, as produced by the repository's
 * query projection. A lightweight carrier — the DTO layer turns {@code totalElements} + the request's
 * page/size into the D14 {@code PageMetadata} envelope.
 */
public final class TrustRelationshipSummaryPage {

    private final List<TrustRelationshipSummary> items;
    private final long totalElements;

    public TrustRelationshipSummaryPage(List<TrustRelationshipSummary> items, long totalElements) {

        this.items = items;
        this.totalElements = totalElements;
    }

    public List<TrustRelationshipSummary> getItems() {

        return items;
    }

    public long getTotalElements() {

        return totalElements;
    }
}
