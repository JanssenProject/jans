package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipPage;
import io.jans.shibboleth.trust.dto.shared.PageMetadata;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrustRelationshipPageMapperTests {

    @Test
    void shouldWrapAnEmptyPage() {

        TrustRelationshipPage page = TrustRelationshipMapper.toPage(List.of(), 1, 20, 0);

        assertThat(page.getItems()).isEmpty();
        PageMetadata meta = page.getPage();
        assertThat(meta.getSize()).isEqualTo(20);
        assertThat(meta.getNumber()).isEqualTo(1);
        assertThat(meta.getTotalElements()).isEqualTo(0);
        assertThat(meta.getTotalPages()).isEqualTo(0);
        assertThat(meta.getNumberOfElements()).isEqualTo(0);
    }

    @Test
    void shouldWrapItemsAsSummariesWithMetadata() {

        UUID firstId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID secondId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        TrustRelationshipPage page = TrustRelationshipMapper.toPage(
            List.of(draftWithId(firstId, "First SP"), draftWithId(secondId, "Second SP")),
            1, 20, 145);

        assertThat(page.getItems()).hasSize(2);
        assertThat(page.getItems().get(0).getId()).isEqualTo(firstId);
        assertThat(page.getItems().get(0).getDisplayName()).isEqualTo("First SP");
        assertThat(page.getItems().get(1).getId()).isEqualTo(secondId);

        PageMetadata meta = page.getPage();
        assertThat(meta.getNumber()).isEqualTo(1);
        assertThat(meta.getSize()).isEqualTo(20);
        assertThat(meta.getTotalElements()).isEqualTo(145);
        assertThat(meta.getTotalPages()).isEqualTo(8);
        assertThat(meta.getNumberOfElements()).isEqualTo(2);
    }

    @Test
    void shouldComputeTotalPagesByCeiling() {

        assertThat(totalPagesFor(40, 20)).isEqualTo(2);
        assertThat(totalPagesFor(41, 20)).isEqualTo(3);
        assertThat(totalPagesFor(20, 20)).isEqualTo(1);
        assertThat(totalPagesFor(1, 20)).isEqualTo(1);
        assertThat(totalPagesFor(0, 20)).isEqualTo(0);
    }

    @Test
    void shouldRejectAnItemWithUnassignedId() {

        TrustRelationship unassigned = TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();

        assertThatThrownBy(() -> TrustRelationshipMapper.toPage(List.of(unassigned), 1, 20, 1))
            .isInstanceOf(IllegalStateException.class);
    }

    private static int totalPagesFor(long totalElements, int size) {

        return TrustRelationshipMapper.toPage(List.of(), 1, size, totalElements).getPage().getTotalPages();
    }

    private static TrustRelationship draftWithId(UUID id, String name) {

        TrustRelationship created = TrustRelationship
            .create(DisplayName.of(name).getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();

        return TrustRelationship.from(created).withId(Id.of(id)).build().getValue();
    }
}
