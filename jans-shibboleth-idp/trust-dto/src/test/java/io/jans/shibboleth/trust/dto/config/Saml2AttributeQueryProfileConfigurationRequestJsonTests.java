package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;

import org.junit.jupiter.api.Test;

class Saml2AttributeQueryProfileConfigurationRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesDurationStringAndEnums() throws Exception {

        String body = "{\"assertion_time_condition\":\"INCLUDE_NOT_BEFORE\",\"assertion_lifetime\":\"PT5M\","
            + "\"friendly_name_randomization_policy\":\"RANDOMIZED\"}";

        Saml2AttributeQueryProfileConfigurationRequest request =
            mapper.readValue(body, Saml2AttributeQueryProfileConfigurationRequest.class);

        assertThat(request.getAssertionTimeCondition()).isEqualTo(AssertionTimeCondition.INCLUDE_NOT_BEFORE);
        assertThat(request.getAssertionLifetime()).isEqualTo("PT5M");
        assertThat(request.getFriendlyNameRandomizationPolicy()).isEqualTo(FriendlyNameRandomizationPolicy.RANDOMIZED);
    }

    @Test
    void leavesOmittedFieldsNull() throws Exception {

        Saml2AttributeQueryProfileConfigurationRequest request = mapper.readValue(
            "{\"assertion_lifetime\":\"PT1H\"}", Saml2AttributeQueryProfileConfigurationRequest.class);

        assertThat(request.getAssertionLifetime()).isEqualTo("PT1H");
        assertThat(request.getStatus()).isNull();
        assertThat(request.getAssertionTimeCondition()).isNull();
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"assertion_lifetime\":\"PT5M\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, Saml2AttributeQueryProfileConfigurationRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
