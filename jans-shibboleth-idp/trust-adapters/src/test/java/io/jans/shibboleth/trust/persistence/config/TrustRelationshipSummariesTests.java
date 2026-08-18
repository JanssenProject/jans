package io.jans.shibboleth.trust.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.search.filter.FilterType;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipSummary;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The query-side projection (TP10): reduced entry → view summary, page assembly, and the filter/offset
 * inputs for {@code findPagedEntries}. No domain aggregate, no DB.
 */
@DisplayName("TrustRelationshipSummaries — query projection")
public class TrustRelationshipSummariesTests {

    private static TrustRelationshipSummaryEntry entry(UUID id, String displayName, String description,
        String nature, String status, int version) {

        TrustRelationshipSummaryEntry entry = new TrustRelationshipSummaryEntry();
        entry.setInum(id.toString());
        entry.setDisplayName(displayName);
        entry.setDescription(description);
        entry.setNature(nature);
        entry.setStatus(status);
        entry.setVersion(version);
        return entry;
    }

    @Test
    @DisplayName("GIVEN a summary entry WHEN projected THEN every summary field is carried")
    public void projectsAllFields() {

        UUID id = UUID.randomUUID();

        TrustRelationshipSummary summary = TrustRelationshipSummaries.toSummary(
            entry(id, "Acme SP", "desc", "AGGREGATE", "READY", 4));

        assertThat(summary.getId()).isEqualTo(id);
        assertThat(summary.getDisplayName()).isEqualTo("Acme SP");
        assertThat(summary.getDescription()).isEqualTo("desc");
        assertThat(summary.getNature()).isEqualTo(TrustNature.AGGREGATE);
        assertThat(summary.getStatus()).isEqualTo(TrustStatus.READY);
        assertThat(summary.getVersion()).isEqualTo(4);
    }

    @Test
    @DisplayName("GIVEN entries and a total WHEN paged THEN items are projected and the total is carried")
    public void assemblesPage() {

        TrustRelationshipSummaryPage page = TrustRelationshipSummaries.toPage(
            List.of(entry(UUID.randomUUID(), "A", "", "AGGREGATE", "DRAFT", 1),
                entry(UUID.randomUUID(), "B", "", "INDIVIDUAL", "ACTIVE", 2)),
            57L);

        assertThat(page.getItems()).hasSize(2);
        assertThat(page.getItems()).extracting(TrustRelationshipSummary::getDisplayName).containsExactly("A", "B");
        assertThat(page.getTotalElements()).isEqualTo(57L);
    }

    @Test
    @DisplayName("GIVEN no filter terms WHEN building a filter THEN it is a presence filter matching all")
    public void filterMatchesAllWhenNoTerms() {

        Filter filter = TrustRelationshipSummaries.toFilter(new TrustRelationshipQuery(null, "  ", 1, 20));

        assertThat(filter.getType()).isEqualTo(FilterType.PRESENCE);
        assertThat(filter.getAttributeName()).isEqualTo("inum");
    }

    @Test
    @DisplayName("GIVEN one term WHEN building a filter THEN it is a substring filter on that attribute")
    public void filterIsSubstringForOneTerm() {

        Filter filter = TrustRelationshipSummaries.toFilter(new TrustRelationshipQuery("acme", null, 1, 20));

        assertThat(filter.getType()).isEqualTo(FilterType.SUBSTRING);
        assertThat(filter.getAttributeName()).isEqualTo("displayName");
        assertThat(filter.getSubAny()).containsExactly("acme");
    }

    @Test
    @DisplayName("GIVEN both terms WHEN building a filter THEN they are ANDed")
    public void filterAndsBothTerms() {

        Filter filter = TrustRelationshipSummaries.toFilter(new TrustRelationshipQuery("acme", "payments", 1, 20));

        assertThat(filter.getType()).isEqualTo(FilterType.AND);
        assertThat(filter.getFilters()).hasSize(2);
        assertThat(filter.getFilters()).extracting(Filter::getAttributeName)
            .containsExactly("displayName", "description");
    }

    @Test
    @DisplayName("GIVEN a 1-based page WHEN computing the offset THEN it is (page-1)*size")
    public void computesOffset() {

        assertThat(TrustRelationshipSummaries.offset(new TrustRelationshipQuery(null, null, 1, 20))).isEqualTo(0);
        assertThat(TrustRelationshipSummaries.offset(new TrustRelationshipQuery(null, null, 3, 20))).isEqualTo(40);
    }
}
