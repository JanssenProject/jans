package io.jans.shibboleth.trust.persistence.config;

import io.jans.orm.search.filter.Filter;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Query-side projection for listing trust relationships (TP10/TP11). Builds the view summary DTO directly
 * from the reduced-attribute {@link TrustRelationshipSummaryEntry} — bypassing the domain aggregate — and
 * assembles the search inputs (attribute set, filter, sort, offset) the repository feeds to
 * {@code findPagedEntries}. This is a query projection, deliberately NOT a {@code dto/mapper} domain↔DTO
 * translation.
 */
public final class TrustRelationshipSummaries {

    /** The only attributes listing needs; passed as {@code ldapReturnAttributes} so the blobs stay unread. */
    public static final String[] SUMMARY_ATTRIBUTES =
        { "inum", "displayName", "description", "jansTrustNature", "jansTrustStatus", "jansTrustVer" };

    /** Default list ordering (D14): by display name. */
    public static final String SORT_BY = "displayName";

    private TrustRelationshipSummaries() {
    }

    public static TrustRelationshipSummary toSummary(TrustRelationshipSummaryEntry entry) {

        TrustRelationshipSummary summary = new TrustRelationshipSummary();
        summary.setId(UUID.fromString(entry.getInum()));
        summary.setDisplayName(entry.getDisplayName());
        summary.setDescription(entry.getDescription());
        summary.setNature(TrustNature.valueOf(entry.getNature()));
        summary.setStatus(TrustStatus.valueOf(entry.getStatus()));
        summary.setVersion(entry.getVersion());
        return summary;
    }

    public static TrustRelationshipSummaryPage toPage(List<TrustRelationshipSummaryEntry> entries, long totalElements) {

        List<TrustRelationshipSummary> items = new ArrayList<>();
        for (TrustRelationshipSummaryEntry entry : entries) {

            items.add(toSummary(entry));
        }
        return new TrustRelationshipSummaryPage(items, totalElements);
    }

    /**
     * Builds the search filter for a query (D14): a substring ("contains") filter per supplied term,
     * ANDed together. With no terms, a presence filter on {@code inum} matches every trust relationship.
     */
    public static Filter toFilter(TrustRelationshipQuery query) {

        List<Filter> filters = new ArrayList<>();
        if (query.hasDisplayNameFilter()) {

            filters.add(Filter.createSubstringFilter("displayName", null,
                new String[] { query.getDisplayNameContains() }, null));
        }
        if (query.hasDescriptionFilter()) {

            filters.add(Filter.createSubstringFilter("description", null,
                new String[] { query.getDescriptionContains() }, null));
        }

        if (filters.isEmpty()) {

            return Filter.createPresenceFilter("inum");
        }
        if (filters.size() == 1) {

            return filters.get(0);
        }
        return Filter.createANDFilter(filters);
    }

    /** 0-based offset for the 1-based page/size (D14). */
    public static int offset(TrustRelationshipQuery query) {

        return Math.max(0, (query.getPage() - 1) * query.getSize());
    }
}
