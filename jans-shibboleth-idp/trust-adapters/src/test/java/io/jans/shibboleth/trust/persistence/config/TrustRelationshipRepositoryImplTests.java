package io.jans.shibboleth.trust.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.error.TrustRelationshipNotFound;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.profile.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.Version;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Repository behaviour against a mocked {@link PersistenceEntryManager} (no DB): id assignment on insert,
 * merge on update, find/rehydrate, not-found, delete, and the list projection wiring.
 */
@DisplayName("TrustRelationshipRepositoryImpl — mocked entry manager")
public class TrustRelationshipRepositoryImplTests {

    private static final String BASE_DN = "ou=trust-relationships,o=jans";

    private final PersistenceEntryManager entryManager = mock(PersistenceEntryManager.class);
    private final TrustRelationshipRepositoryImpl repository =
        new TrustRelationshipRepositoryImpl(entryManager, BASE_DN);

    @Test
    @DisplayName("GIVEN an unassigned-id trust relationship WHEN saved THEN an id is assigned and it is persisted")
    public void saveInsertsAndAssignsId() {

        TrustRelationship draft = TrustRelationship.create(
            io.jans.shibboleth.trust.config.DisplayName.of("Acme SP").getValue(),
            Description.of(""), TrustNature.AGGREGATE).getValue();

        Result<TrustRelationship> saved = repository.save(draft);

        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getValue().getId().isAssigned()).isTrue();

        ArgumentCaptor<TrustRelationshipEntry> captor = ArgumentCaptor.forClass(TrustRelationshipEntry.class);
        verify(entryManager).persist(captor.capture());
        verify(entryManager, never()).merge(any());
        assertThat(captor.getValue().getInum()).isNotNull();
        assertThat(captor.getValue().getDn())
            .startsWith("inum=" + captor.getValue().getInum() + ",")
            .endsWith(BASE_DN);
    }

    @Test
    @DisplayName("GIVEN an assigned-id trust relationship WHEN saved THEN it is merged, keeping its id")
    public void saveUpdatesExisting() {

        UUID id = UUID.randomUUID();
        TrustRelationship existing = aggregate(id);

        Result<TrustRelationship> saved = repository.save(existing);

        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getValue().getId().getValue().getValue()).isEqualTo(id);
        verify(entryManager).merge(any(TrustRelationshipEntry.class));
        verify(entryManager, never()).persist(any());
    }

    @Test
    @DisplayName("GIVEN a stored entry WHEN found by id THEN the aggregate is rehydrated")
    public void findByIdRehydrates() {

        UUID id = UUID.randomUUID();
        TrustRelationship aggregate = aggregate(id);
        TrustRelationshipEntry entry = TrustRelationshipEntryMapper.toEntry(aggregate);
        entry.setDn("inum=" + id + "," + BASE_DN);

        when(entryManager.find(eq("inum=" + id + "," + BASE_DN), eq(TrustRelationshipEntry.class),
            nullable(String[].class))).thenReturn(entry);

        Result<TrustRelationship> found = repository.findById(Id.of(id));

        assertThat(found.isSuccess()).isTrue();
        assertThat(found.getValue()).isEqualTo(aggregate);
    }

    @Test
    @DisplayName("GIVEN no stored entry WHEN found by id THEN it fails with TrustRelationshipNotFound")
    public void findByIdNotFound() {

        when(entryManager.find(any(String.class), eq(TrustRelationshipEntry.class),
            nullable(String[].class))).thenReturn(null);

        Result<TrustRelationship> found = repository.findById(Id.of(UUID.randomUUID()));

        assertThat(found.isFailure()).isTrue();
        assertThat(found.getError()).isInstanceOf(TrustRelationshipNotFound.class);
    }

    @Test
    @DisplayName("GIVEN an id WHEN deleted THEN the entry at its DN is removed")
    public void deleteRemovesEntry() {

        UUID id = UUID.randomUUID();

        Result<Void> result = repository.delete(Id.of(id));

        assertThat(result.isSuccess()).isTrue();
        verify(entryManager).remove("inum=" + id + "," + BASE_DN, TrustRelationshipEntry.class);
    }

    @Test
    @DisplayName("GIVEN a paged query WHEN listed THEN the summary page is projected with filter, sort and offset")
    public void listProjectsSummaryPage() {

        PagedResult<TrustRelationshipSummaryEntry> paged = new PagedResult<>();
        paged.setEntries(List.of(summaryEntry("Acme SP"), summaryEntry("Beacon SP")));
        paged.setTotalEntriesCount(42);

        when(entryManager.findPagedEntries(
            eq(BASE_DN), eq(TrustRelationshipSummaryEntry.class), any(), any(),
            eq(TrustRelationshipSummaries.SORT_BY), eq(SortOrder.ASCENDING), eq(40), eq(20), eq(20)))
            .thenReturn(paged);

        Result<TrustRelationshipSummaryPage> result = repository.list(new TrustRelationshipQuery(null, null, 3, 20));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getItems()).hasSize(2);
        assertThat(result.getValue().getTotalElements()).isEqualTo(42L);
    }

    private static TrustRelationshipSummaryEntry summaryEntry(String displayName) {

        TrustRelationshipSummaryEntry entry = new TrustRelationshipSummaryEntry();
        entry.setInum(UUID.randomUUID().toString());
        entry.setDisplayName(displayName);
        entry.setDescription("");
        entry.setNature("AGGREGATE");
        entry.setStatus("DRAFT");
        entry.setVersion(1);
        return entry;
    }

    private static TrustRelationship aggregate(UUID id) {

        return TrustRelationship.builder()
            .withId(Id.of(id))
            .withDisplayName(io.jans.shibboleth.trust.config.DisplayName.of("Existing SP").getValue())
            .withDescription(Description.of(""))
            .withNature(TrustNature.AGGREGATE)
            .withVersion(Version.initial())
            .withStatus(TrustStatus.DRAFT)
            .withMetadataSource(new NoMetadataSource())
            .withDiscoveredEntityIds(EntityIds.empty())
            .withShibbolethSsoProfileConfiguration(SamlProfileConfigurationDefaults.shibbolethSso())
            .withSaml2ArtifactResolutionProfileConfiguration(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .withSaml2AttributeQueryProfileConfiguration(SamlProfileConfigurationDefaults.saml2AttributeQuery())
            .withSaml2EcpProfileConfiguration(SamlProfileConfigurationDefaults.saml2Ecp())
            .withSaml2SsoProfileConfiguration(SamlProfileConfigurationDefaults.saml2Sso())
            .withSaml2LogoutProfileConfiguration(SamlProfileConfigurationDefaults.saml2Logout())
            .withReleasedAttributes(ReleasedAttributes.empty())
            .withActivationDiagnostics(ActivationDiagnostics.none())
            .build()
            .getValue();
    }
}
