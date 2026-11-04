package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.ReleasedAttributeDto;
import io.jans.shibboleth.trust.dto.config.ReleasedAttributesView;
import io.jans.shibboleth.trust.dto.config.UpdateReleasedAttributesRequest;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrustRelationshipReleasedAttributesViewMapperTests {

    private static final UUID ATTR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void shouldViewEmptyWhenNoneReleased() {

        ReleasedAttributesView view = TrustRelationshipMapper.toReleasedAttributesView(individual());

        assertThat(view.getAttributes()).isEmpty();
    }

    @Test
    void shouldViewConfiguredReleasedAttributes() {

        TrustRelationship withOne = TrustRelationshipMapper.updateReleasedAttributes(
            individual(), new UpdateReleasedAttributesRequest(List.of(new ReleasedAttributeDto(ATTR_ID, "givenName"))))
            .getValue();

        ReleasedAttributesView view = TrustRelationshipMapper.toReleasedAttributesView(withOne);

        assertThat(view.getAttributes()).hasSize(1);
        assertThat(view.getAttributes().get(0).getId()).isEqualTo(ATTR_ID);
        assertThat(view.getAttributes().get(0).getDisplayName()).isEqualTo("givenName");
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
