package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.dto.config.TrustRelationshipSummary;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrustRelationshipMapperTests {

    private static final UUID SOME_ID = UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f");

    // ---- Mapper-in: toDomain(CreateTrustRelationshipRequest) -------------------------------------

    @Test
    void shouldCreateDraftIndividualFromValidRequest() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("University Portal SP", "SAML SP", TrustNature.INDIVIDUAL);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship tr = result.getValue();
        assertThat(tr.getStatus()).isEqualTo(TrustStatus.DRAFT);
        assertThat(tr.getVersion().getValue()).isEqualTo(1);
        assertThat(tr.getNature()).isEqualTo(TrustNature.INDIVIDUAL);
        assertThat(tr.getDisplayName().getValue()).isEqualTo("University Portal SP");
        assertThat(tr.getDescription().getValue()).isEqualTo("SAML SP");
        assertThat(tr.getMetadataSource().getType()).isEqualTo(MetadataSourceType.NONE);
        assertThat(tr.getId().isNotAssigned()).isTrue();
    }

    @Test
    void shouldCreateAggregateFromValidRequest() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("Federation", "agg", TrustNature.AGGREGATE);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getNature()).isEqualTo(TrustNature.AGGREGATE);
    }

    @Test
    void shouldFailWhenDisplayNameIsNull() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest(null, "desc", TrustNature.INDIVIDUAL);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldFailWhenDisplayNameIsBlank() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("   ", "desc", TrustNature.INDIVIDUAL);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    void shouldTrimDisplayName() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("  Portal SP  ", "desc", TrustNature.INDIVIDUAL);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getDisplayName().getValue()).isEqualTo("Portal SP");
    }

    @Test
    void shouldNormaliseNullDescriptionToEmpty() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("Portal SP", null, TrustNature.INDIVIDUAL);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getDescription().getValue()).isEqualTo("");
    }

    @Test
    void shouldTrimDescription() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("Portal SP", "  hi  ", TrustNature.INDIVIDUAL);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getDescription().getValue()).isEqualTo("hi");
    }

    @Test
    void shouldFailWhenNatureIsNull() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("Portal SP", "desc", null);

        Result<TrustRelationship> result = TrustRelationshipMapper.toDomain(request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isNotNull();
    }

    // ---- Mapper-out: toSummary(TrustRelationship) ------------------------------------------------

    @Test
    void shouldProjectDraftTrustRelationshipToSummary() {

        TrustRelationship tr = draftWithId(SOME_ID, "Portal SP", "desc", TrustNature.INDIVIDUAL);

        TrustRelationshipSummary summary = TrustRelationshipMapper.toSummary(tr);

        assertThat(summary.getId()).isEqualTo(SOME_ID);
        assertThat(summary.getDisplayName()).isEqualTo("Portal SP");
        assertThat(summary.getDescription()).isEqualTo("desc");
        assertThat(summary.getNature()).isEqualTo(TrustNature.INDIVIDUAL);
        assertThat(summary.getStatus()).isEqualTo(TrustStatus.DRAFT);
        assertThat(summary.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldRejectUnassignedIdAtTheDtoBoundary() {

        TrustRelationship unassigned = TrustRelationship
            .create(displayName("Portal SP"), Description.of("desc"), TrustNature.INDIVIDUAL)
            .getValue();

        assertThatThrownBy(() -> TrustRelationshipMapper.toSummary(unassigned))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReflectBumpedVersion() {

        TrustRelationship v1 = draftWithId(SOME_ID, "Portal SP", "desc", TrustNature.INDIVIDUAL);
        TrustRelationship v2 = v1.updateDisplayName(displayName("Renamed")).getValue();

        TrustRelationshipSummary summary = TrustRelationshipMapper.toSummary(v2);

        assertThat(summary.getVersion()).isEqualTo(2);
        assertThat(summary.getDisplayName()).isEqualTo("Renamed");
    }

    // ---- Integration: create then summarise ------------------------------------------------------

    @Test
    void shouldRoundTripDescriptiveFieldsThroughCreateThenSummary() {

        CreateTrustRelationshipRequest request =
            new CreateTrustRelationshipRequest("Portal SP", "the description", TrustNature.INDIVIDUAL);

        TrustRelationship created = TrustRelationshipMapper.toDomain(request).getValue();
        TrustRelationship persisted = TrustRelationship.from(created).withId(Id.of(SOME_ID)).build().getValue();

        TrustRelationshipSummary summary = TrustRelationshipMapper.toSummary(persisted);

        assertThat(summary.getDisplayName()).isEqualTo("Portal SP");
        assertThat(summary.getDescription()).isEqualTo("the description");
        assertThat(summary.getNature()).isEqualTo(TrustNature.INDIVIDUAL);
        assertThat(summary.getStatus()).isEqualTo(TrustStatus.DRAFT);
        assertThat(summary.getVersion()).isEqualTo(1);
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static DisplayName displayName(String value) {

        return DisplayName.of(value).getValue();
    }

    private static TrustRelationship draftWithId(UUID id, String name, String description, TrustNature nature) {

        TrustRelationship created = TrustRelationship
            .create(displayName(name), Description.of(description), nature)
            .getValue();

        return TrustRelationship.from(created).withId(Id.of(id)).build().getValue();
    }
}
