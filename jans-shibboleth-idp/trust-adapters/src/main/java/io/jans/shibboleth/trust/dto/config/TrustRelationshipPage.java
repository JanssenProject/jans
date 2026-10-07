package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.dto.shared.PageMetadata;

import java.util.List;
import java.util.Objects;

/**
 * A page of trust relationships (as summaries) together with its pagination metadata.
 */
public class TrustRelationshipPage {

    @JsonProperty("items")
    private final List<TrustRelationshipSummary> items;

    @JsonProperty("page")
    private final PageMetadata page;

    public TrustRelationshipPage(List<TrustRelationshipSummary> items, PageMetadata page) {

        this.items = items;
        this.page = page;
    }

    public List<TrustRelationshipSummary> getItems() {

        return items;
    }

    public PageMetadata getPage() {

        return page;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrustRelationshipPage that = (TrustRelationshipPage) o;
        return Objects.equals(items, that.items) && Objects.equals(page, that.page);
    }

    @Override
    public int hashCode() {

        return Objects.hash(items, page);
    }

    @Override
    public String toString() {

        return "TrustRelationshipPage{items=" + items + ", page=" + page + '}';
    }
}
