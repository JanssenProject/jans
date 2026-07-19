package io.jans.shibboleth.trust.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import io.jans.shibboleth.trust.config.error.DomainObjectCreationFailed;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EntityIds — builder error-accumulation contract")
public class EntityIdsBuilderTests {

    private static EntityId entityId(String uri) {

        return EntityId.of(URI.create(uri)).getValue();
    }

    private static final EntityId FIRST = entityId("https://sp.example.org/first");
    private static final EntityId SECOND = entityId("https://sp.example.org/second");

    @Test
    @DisplayName("GIVEN a builder with two ids added WHEN built THEN it succeeds and reports it has entries")
    public void shouldBuildSuccessfully_whenIdsAdded() {

        Result<EntityIds> result = EntityIds.builder().add(FIRST).add(SECOND).build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().hasAny()).isTrue();
        assertThat(result.getValue().hasNone()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a fresh builder with nothing added WHEN built THEN it succeeds as an empty collection")
    public void shouldBuildEmpty_whenNothingAdded() {

        Result<EntityIds> result = EntityIds.builder().build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().hasNone()).isTrue();
    }

    @Test
    @DisplayName("GIVEN the empty() factory WHEN queried THEN it holds no entries")
    public void shouldHoldNothing_whenEmpty() {

        assertThat(EntityIds.empty().hasNone()).isTrue();
        assertThat(EntityIds.empty().hasAny()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null id added WHEN built THEN it fails, wrapping RequiredValueMissing in DomainObjectCreationFailed")
    public void shouldFail_whenNullAdded() {

        Result<EntityIds> result = EntityIds.builder().add(null).build();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectCreationFailed.class);

        DomainObjectCreationFailed error = (DomainObjectCreationFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN addAll containing a null element WHEN built THEN it fails at the first null with RequiredValueMissing")
    public void shouldFail_whenAddAllContainsNull() {

        List<EntityId> ids = new java.util.ArrayList<>();
        ids.add(FIRST);
        ids.add(null);

        Result<EntityIds> result = EntityIds.builder().addAll(ids).build();

        assertThat(result.isFailure()).isTrue();
        assertThat(((DomainObjectCreationFailed) result.getError()).getCause())
            .isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a builder already in error WHEN a further valid id is added THEN the error is retained and build still fails")
    public void shouldShortCircuit_afterFirstError() {

        Result<EntityIds> result = EntityIds.builder().add(null).add(FIRST).build();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectCreationFailed.class);
    }

    @Test
    @DisplayName("GIVEN an existing EntityIds WHEN a builder is seeded from() it THEN the entries are carried over")
    public void shouldCarryOverEntries_whenBuiltFromExisting() {

        EntityIds existing = EntityIds.builder().add(FIRST).build().getValue();

        Result<EntityIds> result = EntityIds.from(existing).add(SECOND).build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().hasAny()).isTrue();
    }

    @Test
    @DisplayName("GIVEN two EntityIds built from the same ids WHEN compared THEN they are equal and share a hashCode")
    public void shouldBeEqual_whenSameIds() {

        EntityIds one = EntityIds.builder().add(FIRST).add(SECOND).build().getValue();
        EntityIds another = EntityIds.builder().add(FIRST).add(SECOND).build().getValue();

        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }

    @Test
    @DisplayName("GIVEN a built EntityIds WHEN getEntityIds() is read THEN it returns exactly the added ids")
    public void shouldExposeAddedIds_whenRead() {

        EntityIds entityIds = EntityIds.builder().add(FIRST).add(SECOND).build().getValue();

        assertThat(entityIds.getEntityIds()).containsExactlyInAnyOrder(FIRST, SECOND);
    }

    @Test
    @DisplayName("GIVEN the empty() factory WHEN getEntityIds() is read THEN it returns an empty set")
    public void shouldExposeEmptySet_whenEmpty() {

        assertThat(EntityIds.empty().getEntityIds()).isEmpty();
    }

    @Test
    @DisplayName("GIVEN the exposed id set WHEN a caller tries to modify it THEN the set is unmodifiable")
    public void shouldExposeUnmodifiableSet_whenRead() {

        EntityIds entityIds = EntityIds.builder().add(FIRST).build().getValue();

        assertThat(entityIds.getEntityIds()).isUnmodifiable();
    }
}
