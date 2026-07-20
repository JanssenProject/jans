package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.config.UpdateBasicInfoRequest;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.Test;

class TrustRelationshipBasicInfoMapperTests {

    @Test
    void shouldApplyBothFieldsAndBumpVersionOnce() {

        TrustRelationship existing = existing();
        UpdateBasicInfoRequest request = new UpdateBasicInfoRequest("New Name", "new description");

        Result<TrustRelationship> result = TrustRelationshipMapper.updateBasicInfo(existing, request);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated.getDisplayName().getValue()).isEqualTo("New Name");
        assertThat(updated.getDescription().getValue()).isEqualTo("new description");
        assertThat(updated.getVersion()).isEqualTo(existing.getVersion().next());
    }

    @Test
    void shouldNormaliseNullDescriptionToEmpty() {

        UpdateBasicInfoRequest request = new UpdateBasicInfoRequest("New Name", null);

        Result<TrustRelationship> result = TrustRelationshipMapper.updateBasicInfo(existing(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getDescription().getValue()).isEmpty();
    }

    @Test
    void shouldFailWhenDisplayNameIsBlank() {

        UpdateBasicInfoRequest request = new UpdateBasicInfoRequest("   ", "desc");

        Result<TrustRelationship> result = TrustRelationshipMapper.updateBasicInfo(existing(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldFailWhenDisplayNameIsNull() {

        UpdateBasicInfoRequest request = new UpdateBasicInfoRequest(null, "desc");

        Result<TrustRelationship> result = TrustRelationshipMapper.updateBasicInfo(existing(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldNotBumpVersionWhenNothingChanges() {

        TrustRelationship existing = existing();
        UpdateBasicInfoRequest request = new UpdateBasicInfoRequest("Original SP", "original description");

        Result<TrustRelationship> result = TrustRelationshipMapper.updateBasicInfo(existing, request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(existing);
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship existing() {

        return TrustRelationship
            .create(DisplayName.of("Original SP").getValue(), Description.of("original description"),
                TrustNature.INDIVIDUAL)
            .getValue();
    }
}
