package io.jans.shibboleth.trust.dto.mapper.config;

import static io.jans.shibboleth.trust.dto.error.ViolationAssert.assertOnlyViolation;
import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.ReleasedAttributeDto;
import io.jans.shibboleth.trust.dto.config.UpdateReleasedAttributesRequest;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrustRelationshipReleasedAttributesMapperTests {

    private static final UUID ATTR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void shouldReplaceWithProvidedAttributes() {

        UpdateReleasedAttributesRequest request = new UpdateReleasedAttributesRequest(
            List.of(new ReleasedAttributeDto(ATTR_ID, "givenName")));

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateReleasedAttributes(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getReleasedAttributes().getAttributes()).hasSize(1);
        assertThat(result.getValue().getReleasedAttributes().getAttributes())
            .allSatisfy(a -> assertThat(a.getDisplayName()).isEqualTo("givenName"));
    }

    @Test
    void shouldClearWhenGivenAnEmptyList() {

        TrustRelationship withOne = TrustRelationshipMapper.updateReleasedAttributes(
            individual(), new UpdateReleasedAttributesRequest(List.of(new ReleasedAttributeDto(ATTR_ID, "givenName"))))
            .getValue();

        Result<TrustRelationship> cleared = TrustRelationshipMapper.updateReleasedAttributes(
            withOne, new UpdateReleasedAttributesRequest(List.of()));

        assertThat(cleared.isSuccess()).isTrue();
        assertThat(cleared.getValue().getReleasedAttributes().getAttributes()).isEmpty();
    }

    @Test
    void shouldFailWhenAnAttributeHasBlankDisplayName() {

        UpdateReleasedAttributesRequest request = new UpdateReleasedAttributesRequest(
            List.of(new ReleasedAttributeDto(ATTR_ID, "  ")));

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateReleasedAttributes(individual(), request);

        assertOnlyViolation(result, "attributes[0].display_name", "required_value_missing");
    }

    @Test
    void shouldFailWhenAnAttributeHasNoId() {

        UpdateReleasedAttributesRequest request = new UpdateReleasedAttributesRequest(
            List.of(new ReleasedAttributeDto(null, "givenName")));

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateReleasedAttributes(individual(), request);

        assertOnlyViolation(result, "attributes[0].id", "required_value_missing");
    }

    @Test
    void shouldFailWhenAttributesAreMissing() {

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateReleasedAttributes(individual(), new UpdateReleasedAttributesRequest());

        assertOnlyViolation(result, "attributes", "required_value_missing");
    }

    @Test
    void shouldLeaveVersionUnchangedWhenReplacingEmptyWithEmpty() {

        TrustRelationship existing = individual();

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateReleasedAttributes(existing, new UpdateReleasedAttributesRequest(List.of()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
