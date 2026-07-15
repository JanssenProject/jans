package io.jans.shibboleth.trust.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import io.jans.shibboleth.trust.config.error.DomainObjectCreationFailed;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReleasedAttributes — builder error-accumulation contract")
public class ReleasedAttributesBuilderTests {

    private static ReleasedAttribute attribute(String displayName) {

        return ReleasedAttribute.of(Id.generate(), displayName).getValue();
    }

    private static final ReleasedAttribute FIRST = attribute("mail");
    private static final ReleasedAttribute SECOND = attribute("displayName");

    @Test
    @DisplayName("GIVEN a builder with two attributes added WHEN built THEN it succeeds and exposes the entries")
    public void shouldBuildSuccessfully_whenAttributesAdded() {

        Result<ReleasedAttributes> result = ReleasedAttributes.builder().add(FIRST).add(SECOND).build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().hasAny()).isTrue();
        assertThat(result.getValue().getAttributes()).containsExactlyInAnyOrder(FIRST, SECOND);
    }

    @Test
    @DisplayName("GIVEN a fresh builder with nothing added WHEN built THEN it succeeds as an empty collection")
    public void shouldBuildEmpty_whenNothingAdded() {

        Result<ReleasedAttributes> result = ReleasedAttributes.builder().build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().hasNone()).isTrue();
    }

    @Test
    @DisplayName("GIVEN the empty() factory WHEN queried THEN it holds no entries")
    public void shouldHoldNothing_whenEmpty() {

        assertThat(ReleasedAttributes.empty().hasNone()).isTrue();
        assertThat(ReleasedAttributes.empty().hasAny()).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null attribute added WHEN built THEN it fails, wrapping RequiredValueMissing in DomainObjectCreationFailed")
    public void shouldFail_whenNullAdded() {

        Result<ReleasedAttributes> result = ReleasedAttributes.builder().add(null).build();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectCreationFailed.class);

        DomainObjectCreationFailed error = (DomainObjectCreationFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN addAll containing a null element WHEN built THEN it fails at the first null with RequiredValueMissing")
    public void shouldFail_whenAddAllContainsNull() {

        List<ReleasedAttribute> attributes = new java.util.ArrayList<>();
        attributes.add(FIRST);
        attributes.add(null);

        Result<ReleasedAttributes> result = ReleasedAttributes.builder().addAll(attributes).build();

        assertThat(result.isFailure()).isTrue();
        assertThat(((DomainObjectCreationFailed) result.getError()).getCause())
            .isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a builder already in error WHEN a further valid attribute is added THEN the error is retained and build still fails")
    public void shouldShortCircuit_afterFirstError() {

        Result<ReleasedAttributes> result = ReleasedAttributes.builder().add(null).add(FIRST).build();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectCreationFailed.class);
    }

    @Test
    @DisplayName("GIVEN an existing ReleasedAttributes WHEN a builder is seeded from() it THEN the entries are carried over")
    public void shouldCarryOverEntries_whenBuiltFromExisting() {

        ReleasedAttributes existing = ReleasedAttributes.builder().add(FIRST).build().getValue();

        Result<ReleasedAttributes> result = ReleasedAttributes.from(existing).add(SECOND).build();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getAttributes()).containsExactlyInAnyOrder(FIRST, SECOND);
    }

    @Test
    @DisplayName("GIVEN two ReleasedAttributes built from the same attributes WHEN compared THEN they are equal and share a hashCode")
    public void shouldBeEqual_whenSameAttributes() {

        ReleasedAttributes one = ReleasedAttributes.builder().add(FIRST).add(SECOND).build().getValue();
        ReleasedAttributes another = ReleasedAttributes.builder().add(FIRST).add(SECOND).build().getValue();

        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }
}
