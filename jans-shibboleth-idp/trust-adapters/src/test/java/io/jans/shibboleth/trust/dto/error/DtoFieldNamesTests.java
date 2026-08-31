package io.jans.shibboleth.trust.dto.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.kernel.RequiredValueMissing;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@org.junit.jupiter.api.DisplayName("DtoFieldNames — domain fields translated to real request fields")
public class DtoFieldNamesTests {

    private static final String DTO_PACKAGE = "io/jans/shibboleth/trust/dto";

    @Test
    @org.junit.jupiter.api.DisplayName("GIVEN a single-field value object WHEN resolved THEN it names its request field")
    public void resolvesSingleFieldValueObjects() {

        assertThat(DtoFieldNames.resolve(RequiredValueMissing.of(DisplayName.class))).isEqualTo("display_name");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GIVEN a domain field whose wire name differs WHEN resolved THEN the wire name wins")
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
    @org.junit.jupiter.api.DisplayName("GIVEN an error naming no field WHEN resolved THEN nothing is invented")
    public void inventsNothingForErrorsThatNameNoField() {

        assertThat(DtoFieldNames.resolve(InvalidUriSyntax.forValue("nope"))).isEqualTo(DtoFieldNames.UNRESOLVED);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GIVEN every mapped name WHEN checked against the DTOs THEN each really exists")
    public void everyMappedNameExistsOnADto() {

        Set<String> wireFields = wireFieldsOfEveryDto();

        assertThat(wireFields)
            .as("@JsonProperty names discovered on the trust DTOs")
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

        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {

            Path root = Path.of(entry);
            Path dtoRoot = root.resolve(DTO_PACKAGE);

            if (!Files.isDirectory(dtoRoot)) {

                continue;
            }

            try (Stream<Path> files = Files.walk(dtoRoot)) {

                files.filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> collectJsonProperties(root, path, names));
            } catch (IOException e) {

                throw new IllegalStateException("Could not scan " + dtoRoot + " for DTO fields", e);
            }
        }

        return names;
    }

    private static void collectJsonProperties(Path root, Path classFile, Set<String> names) {

        String className = root.relativize(classFile).toString()
            .replace(File.separatorChar, '.')
            .replaceAll("\\.class$", "");

        try {

            Class<?> dto = Class.forName(className, false, DtoFieldNamesTests.class.getClassLoader());

            for (Field field : dto.getDeclaredFields()) {

                JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                if (annotation != null) {

                    names.add(annotation.value());
                }
            }

            // constructor-injected DTOs annotate their parameters instead of their fields
            java.util.Arrays.stream(dto.getDeclaredConstructors())
                .flatMap(constructor -> java.util.Arrays.stream(constructor.getParameters()))
                .map(parameter -> parameter.getAnnotation(JsonProperty.class))
                .filter(java.util.Objects::nonNull)
                .forEach(annotation -> names.add(annotation.value()));
        } catch (ClassNotFoundException | NoClassDefFoundError e) {

            // not loadable in this context; other DTOs still contribute their names
        }
    }
}
