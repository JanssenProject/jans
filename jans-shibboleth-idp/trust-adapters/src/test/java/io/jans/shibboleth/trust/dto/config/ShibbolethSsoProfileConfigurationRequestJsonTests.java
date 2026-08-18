package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;

import org.junit.jupiter.api.Test;

class ShibbolethSsoProfileConfigurationRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesDurationsEnumsAndArrays() throws Exception {

        String body = "{\"max_authentication_age\":\"PT1H\",\"assertion_lifetime\":\"PT5M\","
            + "\"authentication_result_reuse_policy\":\"ALLOW_REUSE\","
            + "\"attribute_statement_policy\":\"INCLUDE_ATTRIBUTE_STATEMENT\","
            + "\"nameid_format_precedence\":[\"urn:a\",\"urn:b\"]}";

        ShibbolethSsoProfileConfigurationRequest request =
            mapper.readValue(body, ShibbolethSsoProfileConfigurationRequest.class);

        assertThat(request.getMaxAuthenticationAge()).isEqualTo("PT1H");
        assertThat(request.getAssertionLifetime()).isEqualTo("PT5M");
        assertThat(request.getAuthenticationResultReusePolicy()).isEqualTo(AuthenticationResultReusePolicy.ALLOW_REUSE);
        assertThat(request.getAttributeStatementPolicy()).isEqualTo(AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT);
        assertThat(request.getNameIdFormatPrecedence()).containsExactly("urn:a", "urn:b");
    }

    @Test
    void leavesOmittedFieldsNull() throws Exception {

        ShibbolethSsoProfileConfigurationRequest request = mapper.readValue(
            "{\"status\":\"ACTIVE\"}", ShibbolethSsoProfileConfigurationRequest.class);

        assertThat(request.getMaxAuthenticationAge()).isNull();
        assertThat(request.getNameIdFormatPrecedence()).isNull();
        assertThat(request.getAttributeStatementPolicy()).isNull();
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"status\":\"ACTIVE\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, ShibbolethSsoProfileConfigurationRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
