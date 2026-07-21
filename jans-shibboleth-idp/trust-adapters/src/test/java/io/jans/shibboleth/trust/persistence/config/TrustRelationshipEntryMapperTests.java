package io.jans.shibboleth.trust.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.metadata.FileMetadataSource;
import io.jans.shibboleth.trust.config.metadata.ManualMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MdqMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UriMetadataSource;
import io.jans.shibboleth.trust.config.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.trust.config.metadata.manual.NoCertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;
import io.jans.shibboleth.trust.config.metadata.manual.SamlX509CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.trust.config.profile.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.trust.config.profile.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.Version;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Minimal round-trip for the aggregate ⇄ entry mapper: a default-shaped trust relationship survives
 * {@code domain → entry → domain} unchanged. Proves the harness and the flat-column fidelity (id,
 * display name, description, nature, status, version). The rich sub-structure is default-seeded on read
 * in this slice; payload round-tripping is added as the matrix widens.
 */
@DisplayName("TrustRelationshipEntryMapper — minimal round-trip")
public class TrustRelationshipEntryMapperTests {

    @Test
    @DisplayName("GIVEN a default persisted trust relationship WHEN mapped to entry and back THEN it is unchanged")
    public void defaultAggregateRoundTrips() {

        TrustRelationship original =
            persisted(UUID.randomUUID(), "Acme SP", "", TrustNature.AGGREGATE, 1, new NoMetadataSource());

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    @Test
    @DisplayName("GIVEN an unassigned-id trust relationship WHEN round-tripped THEN the id stays unassigned")
    public void unassignedIdRoundTrips() {

        TrustRelationship original = TrustRelationship.create(
            io.jans.shibboleth.trust.config.DisplayName.of("Draft SP").getValue(),
            Description.of("in progress"), TrustNature.INDIVIDUAL).getValue();

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue().getId().isAssigned()).isFalse();
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    @Test
    @DisplayName("GIVEN varied flat fields WHEN round-tripped THEN every flat column is carried, not hard-coded")
    public void flatColumnsAreCarried() {

        TrustRelationship original = persisted(
            UUID.randomUUID(), "Payments SP", "Handles payment flows", TrustNature.INDIVIDUAL, 7, new NoMetadataSource());

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    @Test
    @DisplayName("GIVEN a customized profile configuration WHEN round-tripped THEN the customization survives")
    public void customizedProfileRoundTrips() {

        Saml2SsoProfileConfiguration customSaml2Sso =
            Saml2SsoProfileConfiguration.from(SamlProfileConfigurationDefaults.saml2Sso())
                .messageSigningPolicy(MessageSigningPolicy.SIGN_NONE)
                .assertionTimeCondition(AssertionTimeCondition.OMIT_NOT_BEFORE)
                .assertionLifetime(Duration.ofMinutes(15))
                .maximumSPSessionLifetime(Duration.ofHours(8))
                .inboundFlows(InterceptorFlows.of(List.of("mfa", "totp")))
                .nameIdFormatPrecedence(NameIdentifiers.of(
                    List.of("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent")))
                .build()
                .getValue();

        TrustRelationship original = TrustRelationship.builder()
            .withId(Id.of(UUID.randomUUID()))
            .withDisplayName(io.jans.shibboleth.trust.config.DisplayName.of("Custom SP").getValue())
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
            .withSaml2SsoProfileConfiguration(customSaml2Sso)
            .withSaml2LogoutProfileConfiguration(SamlProfileConfigurationDefaults.saml2Logout())
            .withReleasedAttributes(ReleasedAttributes.empty())
            .withActivationDiagnostics(ActivationDiagnostics.none())
            .build()
            .getValue();

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue().getSaml2SsoProfileConfiguration()).isEqualTo(customSaml2Sso);
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("metadataSources")
    @DisplayName("GIVEN a trust relationship with a given metadata source WHEN round-tripped THEN it is unchanged")
    public void metadataSourceRoundTrips(String label, TrustNature nature, MetadataSource metadataSource) {

        TrustRelationship original = persisted(UUID.randomUUID(), "SP " + label, "", nature, 1, metadataSource);

        Result<TrustRelationship> roundTripped =
            TrustRelationshipEntryMapper.toDomain(TrustRelationshipEntryMapper.toEntry(original));

        assertThat(roundTripped.isSuccess()).isTrue();
        assertThat(roundTripped.getValue().getMetadataSource()).isEqualTo(metadataSource);
        assertThat(roundTripped.getValue()).isEqualTo(original);
    }

    static Stream<Arguments> metadataSources() {

        return Stream.of(
            Arguments.of("FILE", TrustNature.INDIVIDUAL,
                FileMetadataSource.of("/opt/idp/metadata/sp.xml").getValue()),
            Arguments.of("URI", TrustNature.AGGREGATE,
                UriMetadataSource.of(URI.create("https://sp.example.org/metadata")).getValue()),
            Arguments.of("MDQ", TrustNature.AGGREGATE,
                MdqMetadataSource.of(URI.create("https://mdq.example.org")).getValue()),
            Arguments.of("UPSTREAM", TrustNature.INDIVIDUAL,
                UpstreamMetadataSource.of(Id.of(UUID.randomUUID()),
                    EntityId.of(URI.create("https://idp.example.org/idp")).getValue()).getValue()),
            Arguments.of("MANUAL/X509", TrustNature.INDIVIDUAL, manual(true)),
            Arguments.of("MANUAL/none", TrustNature.INDIVIDUAL, manual(false)));
    }

    private static MetadataSource manual(boolean withCertificate) {

        ManualMetadataSource.Builder builder = ManualMetadataSource.builder()
            .entityId(EntityId.of(URI.create("https://sp.example.org/sp")).getValue())
            .validUntil(ValidityPeriod.until(Instant.parse("2027-01-01T00:00:00Z")).getValue())
            .assertionConsumerService(AssertionConsumerService.of(
                URI.create("https://sp.example.org/acs"), SamlBinding.HTTP_POST, 1, true).getValue())
            .signingCertificate(withCertificate
                ? SamlX509CertificateInfo.fromBase64CertificateData(
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA").getValue()
                : new NoCertificateInfo());

        return builder.build().getValue();
    }

    /** A persisted (assigned-id) trust relationship with default sub-structure, built via the no-bump path. */
    private static TrustRelationship persisted(UUID id, String displayName, String description,
        TrustNature nature, int version, MetadataSource metadataSource) {

        return TrustRelationship.builder()
            .withId(Id.of(id))
            .withDisplayName(io.jans.shibboleth.trust.config.DisplayName.of(displayName).getValue())
            .withDescription(Description.of(description))
            .withNature(nature)
            .withVersion(Version.of(version))
            .withStatus(TrustStatus.DRAFT)
            .withMetadataSource(metadataSource)
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
