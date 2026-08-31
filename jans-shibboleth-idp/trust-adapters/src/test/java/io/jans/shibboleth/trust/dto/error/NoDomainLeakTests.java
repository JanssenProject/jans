package io.jans.shibboleth.trust.dto.error;

import static io.jans.shibboleth.trust.dto.error.ViolationAssert.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.jans.adapter.error.RequestValidationFailed;
import io.jans.adapter.error.Violation;
import io.jans.kernel.Result;
import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.dto.activation.ActivationDiagnosticsRequest;
import io.jans.shibboleth.trust.dto.activation.ActivationLogEntryRequest;
import io.jans.shibboleth.trust.dto.config.AssertionConsumerServiceRequest;
import io.jans.shibboleth.trust.dto.config.CreateTrustRelationshipRequest;
import io.jans.shibboleth.trust.dto.config.FileMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.ManualMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.MdqMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.ReleasedAttributeDto;
import io.jans.shibboleth.trust.dto.config.Saml2SsoProfileConfigurationRequest;
import io.jans.shibboleth.trust.dto.config.UpdateBasicInfoRequest;
import io.jans.shibboleth.trust.dto.config.UpdateReleasedAttributesRequest;
import io.jans.shibboleth.trust.dto.config.UpstreamMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.config.UriMetadataSourceRequest;
import io.jans.shibboleth.trust.dto.mapper.activation.ActivationDiagnosticsMapper;
import io.jans.shibboleth.trust.dto.mapper.activation.WorkerMapper;
import io.jans.shibboleth.trust.dto.mapper.config.TrustRelationshipMapper;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Guards the promise that clients never see the shape of the domain.
 *
 * <p>Every request mapper is driven into failure and its violations inspected. A field or message
 * containing a domain type name, a domain field name, or camelCase at all means the boundary let an
 * implementation detail through — the exact leak the mapping in {@link DtoFieldNames} exists to
 * prevent, and the one that returns the moment someone forwards
 * {@link io.jans.kernel.DomainError#getMessage()} to a client.
 */
@DisplayName("Request mappers — no domain vocabulary reaches clients")
public class NoDomainLeakTests {

    /**
     * Domain vocabulary that must never appear in a violation. These are real identifiers from the
     * trust domain, including the constructor parameter name that used to be reported as a field.
     */
    private static final List<String> DOMAIN_WORDS = Arrays.asList(
        "rawValue", "DisplayName", "TrustRelationship", "BuildContext", "ValidityPeriod",
        "ConfigurationSupport", "MetadataSource", "ReleasedAttribute", "ActivationDiagnostics",
        "EntityId", "WorkerId", "io.jans");

    @Test
    @DisplayName("GIVEN every failing request mapper WHEN its violations are read THEN none names the domain")
    public void violationsNeverNameTheDomain() {

        List<Violation> everyViolation = new ArrayList<>();

        for (Supplier<Result<?>> failing : failingRequests()) {

            List<Violation> violations = violationsOf(failing.get());

            assertThat(violations)
                .as("a failed request must report at least one violation")
                .isNotEmpty();

            everyViolation.addAll(violations);
        }

        for (Violation violation : everyViolation) {

            assertThat(violation.getField())
                .as("violation field must name a request-body field: " + violation)
                .isNotEmpty()
                .matches("[a-z][a-z0-9_]*(\\[\\d+\\])?(\\.[a-z][a-z0-9_]*(\\[\\d+\\])?)*");

            for (String word : DOMAIN_WORDS) {

                assertThat(violation.getField())
                    .as("violation field leaks domain vocabulary '" + word + "': " + violation)
                    .doesNotContain(word);

                assertThat(violation.getMessage())
                    .as("violation message leaks domain vocabulary '" + word + "': " + violation)
                    .doesNotContain(word);
            }
        }
    }

    @Test
    @DisplayName("GIVEN several bad fields in one request WHEN mapped THEN all are reported together")
    public void independentFieldsAreReportedTogether() {

        ManualMetadataSourceRequest request = new ManualMetadataSourceRequest();
        request.setEntityId(null);
        request.setValidUntil("not-a-timestamp");
        request.setAssertionConsumerService(null);

        Result<TrustRelationship> result = TrustRelationshipMapper.updateMetadataSource(individual(), request);

        assertThat(violationsOf(result))
            .extracting(Violation::getField)
            .containsExactlyInAnyOrder("entity_id", "valid_until", "assertion_consumer_service");
    }

    @Test
    @DisplayName("GIVEN several bad attributes WHEN mapped THEN each is reported at its own index")
    public void collectionElementsAreReportedByIndex() {

        UpdateReleasedAttributesRequest request = new UpdateReleasedAttributesRequest(Arrays.asList(
            new ReleasedAttributeDto(null, "givenName"),
            new ReleasedAttributeDto(UUID.fromString("11111111-1111-1111-1111-111111111111"), "  ")));

        Result<TrustRelationship> result = TrustRelationshipMapper.updateReleasedAttributes(individual(), request);

        assertThat(violationsOf(result))
            .extracting(Violation::getField)
            .containsExactlyInAnyOrder("attributes[0].id", "attributes[1].display_name");
    }

    private static List<Supplier<Result<?>>> failingRequests() {

        List<Supplier<Result<?>>> requests = new ArrayList<>();

        requests.add(() -> TrustRelationshipMapper.toDomain(
            new CreateTrustRelationshipRequest("  ", "desc", TrustNature.INDIVIDUAL)));

        requests.add(() -> TrustRelationshipMapper.updateBasicInfo(
            individual(), new UpdateBasicInfoRequest(null, "desc")));

        requests.add(() -> TrustRelationshipMapper.updateMetadataSource(
            individual(), new UriMetadataSourceRequest("not a uri")));

        requests.add(() -> TrustRelationshipMapper.updateMetadataSource(
            individual(), new UriMetadataSourceRequest(null)));

        requests.add(() -> TrustRelationshipMapper.updateMetadataSource(
            individual(), new MdqMetadataSourceRequest(null)));

        requests.add(() -> TrustRelationshipMapper.updateMetadataSource(
            individual(), new FileMetadataSourceRequest(null)));

        requests.add(() -> TrustRelationshipMapper.updateMetadataSource(
            individual(), new UpstreamMetadataSourceRequest("not-a-uuid", "https://sp.example.org")));

        requests.add(() -> TrustRelationshipMapper.updateMetadataSource(individual(), manualWithBadAcs()));

        requests.add(() -> TrustRelationshipMapper.updateReleasedAttributes(
            individual(), new UpdateReleasedAttributesRequest()));

        requests.add(() -> TrustRelationshipMapper.updateReleasedAttributes(individual(),
            new UpdateReleasedAttributesRequest(List.of(new ReleasedAttributeDto(null, "givenName")))));

        requests.add(() -> {

            Saml2SsoProfileConfigurationRequest profile = new Saml2SsoProfileConfigurationRequest();
            profile.setAssertionLifetime("not-a-duration");
            return TrustRelationshipMapper.updateSaml2SsoProfileConfiguration(individual(), profile);
        });

        requests.add(() -> ActivationDiagnosticsMapper.toDomain(diagnosticsWithBadTimestamps()));

        requests.add(() -> WorkerMapper.toWorkerId("  "));

        return requests;
    }

    private static ManualMetadataSourceRequest manualWithBadAcs() {

        ManualMetadataSourceRequest request = new ManualMetadataSourceRequest();
        request.setEntityId("https://sp.example.org");
        request.setValidUntil("2030-01-01T00:00:00Z");
        request.setAssertionConsumerService(new AssertionConsumerServiceRequest());
        return request;
    }

    private static ActivationDiagnosticsRequest diagnosticsWithBadTimestamps() {

        ActivationDiagnosticsRequest request = new ActivationDiagnosticsRequest();
        request.setStatus(ActivationStatus.SUCCEEDED);
        request.setOrigin("worker-1");
        request.setStartedAt("nope");
        request.setCompletedAt("also-nope");
        request.setLogEntries(List.of(new ActivationLogEntryRequest("bad", LogLevel.ERROR, "boom")));
        return request;
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(io.jans.shibboleth.trust.config.DisplayName.of("SP").getValue(), Description.of("desc"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
