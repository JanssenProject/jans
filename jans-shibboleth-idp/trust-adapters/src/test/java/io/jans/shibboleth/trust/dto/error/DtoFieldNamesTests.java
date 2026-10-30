package io.jans.shibboleth.trust.dto.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.kernel.RequiredValueMissing;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@DisplayName("DtoFieldNames — domain fields translated to real request fields")
public class DtoFieldNamesTests {

    private static final String DTO_PACKAGE = "io/jans/shibboleth/trust/dto";

    @Test
    @DisplayName("GIVEN a single-field value object WHEN resolved THEN it names its request field")
    public void resolvesSingleFieldValueObjects() {

        // fully qualified: the simple name collides with JUnit's @DisplayName
        Class<?> displayName = io.jans.shibboleth.trust.config.DisplayName.class;

        assertThat(DtoFieldNames.resolve(RequiredValueMissing.of(displayName))).isEqualTo("display_name");
    }

    @Test
    @DisplayName("GIVEN a domain field whose wire name differs WHEN resolved THEN the wire name wins")
    public void resolvesNamesAConventionWouldGetWrong() {

        // a camel-to-snake convention would emit maximum_s_p_session_lifetime and
        // name_id_format_precedence, neither of which exists in the API
        assertThat(DtoFieldNames.resolve(
            RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "maximumSPSessionLifetime")))
            .isEqualTo("maximum_sp_session_lifetime");

        assertThat(DtoFieldNames.resolve(
            RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "nameIdFormatPrecedence")))
            .isEqualTo("nameid_format_precedence");
    }

    @Test
    @DisplayName("GIVEN an error naming no field WHEN resolved THEN nothing is invented")
    public void inventsNothingForErrorsThatNameNoField() {

        assertThat(DtoFieldNames.resolve(InvalidUriSyntax.forValue("nope"))).isEqualTo(DtoFieldNames.UNRESOLVED);
    }

    @Test
    @DisplayName("GIVEN every mapped name WHEN checked against the DTOs THEN each really exists")
    public void everyMappedNameExistsOnADto() {

        Set<String> wireFields = wireFieldsOfEveryDto();

        // as in the error-type scan: name known fields so the scan cannot pass by reaching nothing,
        // whichever form the DTOs are on the classpath in
        assertThat(wireFields)
            .as("@JsonProperty names discovered on the trust DTOs")
            .contains("display_name", "metadata_source", "assertion_consumer_service", "log_entries")
            .hasSizeGreaterThan(30);

        List<String> unknown = new ArrayList<>();
        for (String mapped : DtoFieldNames.mappedDtoFields()) {

            for (String segment : mapped.split("\\.")) {

                if (!wireFields.contains(segment)) {

                    unknown.add(mapped + " (segment '" + segment + "')");
                }
            }
        }

        assertThat(unknown)
            .as("names in DtoFieldNames that no DTO declares — a client would be pointed at a "
                + "field that does not exist")
            .isEmpty();
    }

    private static Set<String> wireFieldsOfEveryDto() {

        Set<String> names = new HashSet<>();

        for (Class<?> dto : ClasspathClasses.under(Collections.singletonList(DTO_PACKAGE))) {

            for (Field field : dto.getDeclaredFields()) {

                addName(field.getAnnotation(JsonProperty.class), names);
            }

            // constructor-injected DTOs annotate their parameters instead of their fields
            for (Constructor<?> constructor : dto.getDeclaredConstructors()) {

                for (Parameter parameter : constructor.getParameters()) {

                    addName(parameter.getAnnotation(JsonProperty.class), names);
                }
            }
        }

        return names;
    }

    private static void addName(JsonProperty annotation, Set<String> names) {

        if (annotation != null) {

            names.add(annotation.value());
        }
    }
}
