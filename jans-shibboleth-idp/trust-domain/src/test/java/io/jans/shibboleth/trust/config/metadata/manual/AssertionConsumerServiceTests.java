package io.jans.shibboleth.trust.config.metadata.manual;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AssertionConsumerService — construction & accessors")
public class AssertionConsumerServiceTests {

    private static final URI LOCATION = URI.create("https://sp.example.org/acs");

    @Test
    @DisplayName("GIVEN all fields WHEN of() is called THEN it succeeds and exposes each of them")
    public void shouldExposeAllFields() {

        Result<AssertionConsumerService> result =
            AssertionConsumerService.of(LOCATION, SamlBinding.HTTP_POST, 3, false);

        assertThat(result.isSuccess()).isTrue();
        AssertionConsumerService acs = result.getValue();
        assertThat(acs.getLocation()).isEqualTo(LOCATION);
        assertThat(acs.getBinding()).isEqualTo(SamlBinding.HTTP_POST);
        assertThat(acs.getIndex()).isEqualTo(3);
        assertThat(acs.isDefault()).isFalse();
    }

    @Test
    @DisplayName("GIVEN only location and binding WHEN of() is called THEN index defaults to 1 and isDefault to true")
    public void shouldApplyDefaults_whenOnlyLocationAndBinding() {

        AssertionConsumerService acs =
            AssertionConsumerService.of(LOCATION, SamlBinding.HTTP_REDIRECT).getValue();

        assertThat(acs.getIndex()).isEqualTo(1);
        assertThat(acs.isDefault()).isTrue();
        assertThat(acs.getBinding()).isEqualTo(SamlBinding.HTTP_REDIRECT);
    }

    @Test
    @DisplayName("GIVEN a null location WHEN of() is called THEN it fails with RequiredValueMissing")
    public void shouldFail_whenLocationIsNull() {

        Result<AssertionConsumerService> result =
            AssertionConsumerService.of(null, SamlBinding.HTTP_POST);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null binding WHEN of() is called THEN it fails with RequiredValueMissing")
    public void shouldFail_whenBindingIsNull() {

        Result<AssertionConsumerService> result =
            AssertionConsumerService.of(LOCATION, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
