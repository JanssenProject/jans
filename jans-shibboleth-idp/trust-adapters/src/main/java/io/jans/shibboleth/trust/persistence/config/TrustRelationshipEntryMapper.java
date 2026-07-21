package io.jans.shibboleth.trust.persistence.config;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
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
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UriMetadataSource;
import io.jans.shibboleth.trust.config.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.trust.config.metadata.manual.CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.NoCertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;
import io.jans.shibboleth.trust.config.metadata.manual.SamlX509CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.trust.config.profile.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.trust.config.profile.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;
import io.jans.shibboleth.trust.persistence.config.payload.MetadataSourcePayload;
import io.jans.shibboleth.trust.persistence.config.payload.ProfilesPayload;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.shared.Version;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Translates between the {@link TrustRelationship} aggregate and its {@link TrustRelationshipEntry}.
 *
 * <p>Rehydration ({@link #toDomain}) rebuilds a validated aggregate via the domain's no-bump
 * {@code builder()} path. This slice round-trips the flat columns and default-seeds the rich
 * sub-structure (metadata {@code NONE}, default profiles, empty released attributes, no diagnostics);
 * payload serialization for customized sub-structure is added as the round-trip matrix widens.
 */
public final class TrustRelationshipEntryMapper {

    private TrustRelationshipEntryMapper() {
    }

    public static TrustRelationshipEntry toEntry(TrustRelationship trustRelationship) {

        TrustRelationshipEntry entry = new TrustRelationshipEntry();

        Id id = trustRelationship.getId();
        entry.setId(id.isAssigned() ? id.getValue().getValue().toString() : null);
        entry.setDisplayName(trustRelationship.getDisplayName().getValue());
        entry.setDescription(trustRelationship.getDescription().getValue());
        entry.setNature(trustRelationship.getNature().name());
        entry.setStatus(trustRelationship.getStatus().name());
        entry.setVersion(trustRelationship.getVersion().getValue());
        entry.setMetadataSource(toPayload(trustRelationship.getMetadataSource()));
        entry.setProfiles(toProfilesPayload(trustRelationship));

        return entry;
    }

    public static Result<TrustRelationship> toDomain(TrustRelationshipEntry entry) {

        Result<DisplayName> displayName = DisplayName.of(entry.getDisplayName());
        if (displayName.isFailure()) {

            return Result.failure(displayName.getError());
        }

        Result<MetadataSource> metadataSource = toMetadataSource(entry.getMetadataSource());
        if (metadataSource.isFailure()) {

            return Result.failure(metadataSource.getError());
        }

        ProfilesPayload profiles = entry.getProfiles();

        Result<ShibbolethSsoProfileConfiguration> shibbolethSso = toShibbolethSso(profiles.shibbolethSso);
        if (shibbolethSso.isFailure()) {

            return Result.failure(shibbolethSso.getError());
        }

        Result<Saml2SsoProfileConfiguration> saml2Sso = toSaml2Sso(profiles.saml2Sso);
        if (saml2Sso.isFailure()) {

            return Result.failure(saml2Sso.getError());
        }

        Result<Saml2EcpProfileConfiguration> saml2Ecp = toSaml2Ecp(profiles.saml2Ecp);
        if (saml2Ecp.isFailure()) {

            return Result.failure(saml2Ecp.getError());
        }

        Result<Saml2AttributeQueryProfileConfiguration> saml2AttributeQuery =
            toSaml2AttributeQuery(profiles.saml2AttributeQuery);
        if (saml2AttributeQuery.isFailure()) {

            return Result.failure(saml2AttributeQuery.getError());
        }

        Result<Saml2ArtifactResolutionProfileConfiguration> saml2ArtifactResolution =
            toSaml2ArtifactResolution(profiles.saml2ArtifactResolution);
        if (saml2ArtifactResolution.isFailure()) {

            return Result.failure(saml2ArtifactResolution.getError());
        }

        Result<Saml2LogoutProfileConfiguration> saml2Logout = toSaml2Logout(profiles.saml2Logout);
        if (saml2Logout.isFailure()) {

            return Result.failure(saml2Logout.getError());
        }

        Id id = entry.getId() == null ? Id.unassigned() : Id.of(UUID.fromString(entry.getId()));

        return TrustRelationship.builder()
            .withId(id)
            .withDisplayName(displayName.getValue())
            .withDescription(Description.of(entry.getDescription()))
            .withNature(TrustNature.valueOf(entry.getNature()))
            .withVersion(Version.of(entry.getVersion()))
            .withStatus(TrustStatus.valueOf(entry.getStatus()))
            .withMetadataSource(metadataSource.getValue())
            .withDiscoveredEntityIds(EntityIds.empty())
            .withShibbolethSsoProfileConfiguration(shibbolethSso.getValue())
            .withSaml2ArtifactResolutionProfileConfiguration(saml2ArtifactResolution.getValue())
            .withSaml2AttributeQueryProfileConfiguration(saml2AttributeQuery.getValue())
            .withSaml2EcpProfileConfiguration(saml2Ecp.getValue())
            .withSaml2SsoProfileConfiguration(saml2Sso.getValue())
            .withSaml2LogoutProfileConfiguration(saml2Logout.getValue())
            .withReleasedAttributes(ReleasedAttributes.empty())
            .withActivationDiagnostics(ActivationDiagnostics.none())
            .build();
    }

    private static MetadataSourcePayload toPayload(MetadataSource source) {

        MetadataSourcePayload payload = new MetadataSourcePayload();
        payload.setType(source.getType().name());

        switch (source.getType()) {

            case NONE:
                break;
            case FILE:
                payload.setFilePath(((FileMetadataSource) source).getFilePath());
                break;
            case URI:
                payload.setUri(((UriMetadataSource) source).getUri().toString());
                break;
            case MDQ:
                payload.setBaseUrl(((MdqMetadataSource) source).getBaseUrl().toString());
                break;
            case UPSTREAM:
                UpstreamMetadataSource upstream = (UpstreamMetadataSource) source;
                payload.setParentId(upstream.getParentId().getValue().getValue().toString());
                payload.setEntityId(upstream.getEntityId().getValue().toString());
                break;
            case MANUAL:
                ManualMetadataSource manual = (ManualMetadataSource) source;
                payload.setEntityId(manual.getEntityId().getValue().toString());
                payload.setValidUntil(manual.getValidUntil().getValidUntil().toString());
                payload.setAcs(toAcsPayload(manual.getAssertionConsumerService()));
                payload.setSigningCert(toCertPayload(manual.getSigningCertificate()));
                break;
            default:
                throw new IllegalStateException("unhandled metadata source type: " + source.getType());
        }

        return payload;
    }

    private static MetadataSourcePayload.Acs toAcsPayload(AssertionConsumerService acs) {

        MetadataSourcePayload.Acs payload = new MetadataSourcePayload.Acs();
        payload.setLocation(acs.getLocation().toString());
        payload.setBinding(acs.getBinding().name());
        payload.setIndex(acs.getIndex());
        payload.setDefaultEndpoint(acs.isDefault());
        return payload;
    }

    private static MetadataSourcePayload.Cert toCertPayload(CertificateInfo certificate) {

        MetadataSourcePayload.Cert payload = new MetadataSourcePayload.Cert();
        if (certificate.hasCertificateData()) {

            payload.setType("X509");
            payload.setData(certificate.getCertificateData().getValue());
        } else {

            payload.setType("NONE");
        }
        return payload;
    }

    private static Result<MetadataSource> toMetadataSource(MetadataSourcePayload payload) {

        MetadataSourceType type = MetadataSourceType.valueOf(payload.getType());

        switch (type) {

            case NONE:
                return Result.success(new NoMetadataSource());
            case FILE:
                return FileMetadataSource.of(payload.getFilePath());
            case URI:
                return UriMetadataSource.of(URI.create(payload.getUri()));
            case MDQ:
                return asMetadataSource(MdqMetadataSource.of(URI.create(payload.getBaseUrl())));
            case UPSTREAM:
                return toUpstream(payload);
            case MANUAL:
                return toManual(payload);
            default:
                throw new IllegalStateException("unhandled metadata source type: " + type);
        }
    }

    private static Result<MetadataSource> asMetadataSource(Result<MdqMetadataSource> result) {

        if (result.isFailure()) {

            return Result.failure(result.getError());
        }
        return Result.success(result.getValue());
    }

    private static Result<MetadataSource> toUpstream(MetadataSourcePayload payload) {

        Result<EntityId> entityId = EntityId.of(URI.create(payload.getEntityId()));
        if (entityId.isFailure()) {

            return Result.failure(entityId.getError());
        }

        Id parentId = Id.of(UUID.fromString(payload.getParentId()));
        return UpstreamMetadataSource.of(parentId, entityId.getValue());
    }

    private static Result<MetadataSource> toManual(MetadataSourcePayload payload) {

        Result<EntityId> entityId = EntityId.of(URI.create(payload.getEntityId()));
        if (entityId.isFailure()) {

            return Result.failure(entityId.getError());
        }

        Result<ValidityPeriod> validUntil = ValidityPeriod.until(Instant.parse(payload.getValidUntil()));
        if (validUntil.isFailure()) {

            return Result.failure(validUntil.getError());
        }

        MetadataSourcePayload.Acs acsPayload = payload.getAcs();
        Result<AssertionConsumerService> acs = AssertionConsumerService.of(
            URI.create(acsPayload.getLocation()), SamlBinding.valueOf(acsPayload.getBinding()),
            acsPayload.getIndex(), acsPayload.isDefaultEndpoint());
        if (acs.isFailure()) {

            return Result.failure(acs.getError());
        }

        Result<CertificateInfo> certificate = toCertificate(payload.getSigningCert());
        if (certificate.isFailure()) {

            return Result.failure(certificate.getError());
        }

        return ManualMetadataSource.builder()
            .entityId(entityId.getValue())
            .validUntil(validUntil.getValue())
            .assertionConsumerService(acs.getValue())
            .signingCertificate(certificate.getValue())
            .build();
    }

    private static Result<CertificateInfo> toCertificate(MetadataSourcePayload.Cert certificate) {

        if ("X509".equals(certificate.getType())) {

            return SamlX509CertificateInfo.fromBase64CertificateData(certificate.getData());
        }
        return Result.success(new NoCertificateInfo());
    }

    private static ProfilesPayload toProfilesPayload(TrustRelationship trustRelationship) {

        ProfilesPayload payload = new ProfilesPayload();
        payload.shibbolethSso = toPayload(trustRelationship.getShibbolethSsoProfileConfiguration());
        payload.saml2Sso = toPayload(trustRelationship.getSaml2SsoProfileConfiguration());
        payload.saml2Ecp = toPayload(trustRelationship.getSaml2EcpProfileConfiguration());
        payload.saml2AttributeQuery = toPayload(trustRelationship.getSaml2AttributeQueryProfileConfiguration());
        payload.saml2ArtifactResolution = toPayload(trustRelationship.getSaml2ArtifactResolutionProfileConfiguration());
        payload.saml2Logout = toPayload(trustRelationship.getSaml2LogoutProfileConfiguration());
        return payload;
    }

    private static ProfilesPayload.ShibbolethSso toPayload(ShibbolethSsoProfileConfiguration profile) {

        ProfilesPayload.ShibbolethSso payload = new ProfilesPayload.ShibbolethSso();
        payload.status = profile.getStatus().name();
        payload.inboundFlows = profile.getInboundFlows().getFlows();
        payload.outboundFlows = profile.getOutboundFlows().getFlows();
        payload.postAuthenticationFlows = profile.getPostAuthenticationFlows().getFlows();
        payload.maxAuthenticationAge = profile.getMaxAuthenticationAge().toString();
        payload.authenticationResultReusePolicy = profile.getAuthenticationResultReusePolicy().name();
        payload.messageSigningPolicy = profile.getMessageSigningPolicy().name();
        payload.assertionTimeCondition = profile.getAssertionTimeCondition().name();
        payload.assertionLifetime = profile.getAssertionLifetime().toString();
        payload.assertionSigningPolicy = profile.getAssertionSigningPolicy().name();
        payload.attributeStatementPolicy = profile.getAttributeStatementPolicy().name();
        payload.nameIdFormatPrecedence = profile.getNameIdFormatPrecedence().getNameIdentifiers();
        return payload;
    }

    private static Result<ShibbolethSsoProfileConfiguration> toShibbolethSso(ProfilesPayload.ShibbolethSso payload) {

        return ShibbolethSsoProfileConfiguration.from(SamlProfileConfigurationDefaults.shibbolethSso())
            .status(ProfileStatus.valueOf(payload.status))
            .inboundFlows(InterceptorFlows.of(payload.inboundFlows))
            .outboundFlows(InterceptorFlows.of(payload.outboundFlows))
            .postAuthenticationFlows(InterceptorFlows.of(payload.postAuthenticationFlows))
            .maximumAuthenticationAge(Duration.parse(payload.maxAuthenticationAge))
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.valueOf(payload.authenticationResultReusePolicy))
            .messageSigningPolicy(MessageSigningPolicy.valueOf(payload.messageSigningPolicy))
            .assertionTimeCondition(AssertionTimeCondition.valueOf(payload.assertionTimeCondition))
            .assertionLifetime(Duration.parse(payload.assertionLifetime))
            .assertionSigningPolicy(AssertionSigningPolicy.valueOf(payload.assertionSigningPolicy))
            .attributeStatementPolicy(AttributeStatementPolicy.valueOf(payload.attributeStatementPolicy))
            .nameIdFormatPrecedence(NameIdentifiers.of(payload.nameIdFormatPrecedence))
            .build();
    }

    private static ProfilesPayload.Saml2Sso toPayload(Saml2SsoProfileConfiguration profile) {

        ProfilesPayload.Saml2Sso payload = new ProfilesPayload.Saml2Sso();
        payload.status = profile.getStatus().name();
        payload.inboundFlows = profile.getInboundFlows().getFlows();
        payload.outboundFlows = profile.getOutboundFlows().getFlows();
        payload.postAuthenticationFlows = profile.getPostAuthenticationFlows().getFlows();
        payload.maxAuthenticationAge = profile.getMaxAuthenticationAge().toString();
        payload.authenticationResultReusePolicy = profile.getAuthenticationResultReusePolicy().name();
        payload.messageSigningPolicy = profile.getMessageSigningPolicy().name();
        payload.requestSignatureValidationPolicy = profile.getRequestSignatureValidationPolicy().name();
        payload.encryptionFallbackPolicy = profile.getEncryptionFallbackPolicy().name();
        payload.nameIdEncryptionPolicy = profile.getNameIdEncryptionPolicy().name();
        payload.assertionTimeCondition = profile.getAssertionTimeCondition().name();
        payload.assertionLifetime = profile.getAssertionLifetime().toString();
        payload.assertionSigningPolicy = profile.getAssertionSigningPolicy().name();
        payload.assertionEncryptionPolicy = profile.getAssertionEncryptionPolicy().name();
        payload.attributeEncryptionPolicy = profile.getAttributeEncryptionPolicy().name();
        payload.maximumSpSessionLifetime = profile.getMaximumSPSessionLifetime().toString();
        payload.endpointValidationPolicy = profile.getEndpointValidationPolicy().name();
        payload.attributeStatementPolicy = profile.getAttributeStatementPolicy().name();
        payload.friendlyNameRandomizationPolicy = profile.getFriendlyNameRandomizationPolicy().name();
        payload.nameIdFormatPrecedence = profile.getNameIdFormatPrecedence().getNameIdentifiers();
        payload.requestSigningRequirement = profile.getRequestSigningRequirement().name();
        return payload;
    }

    private static Result<Saml2SsoProfileConfiguration> toSaml2Sso(ProfilesPayload.Saml2Sso payload) {

        return Saml2SsoProfileConfiguration.from(SamlProfileConfigurationDefaults.saml2Sso())
            .status(ProfileStatus.valueOf(payload.status))
            .inboundFlows(InterceptorFlows.of(payload.inboundFlows))
            .outboundFlows(InterceptorFlows.of(payload.outboundFlows))
            .postAuthenticationFlows(InterceptorFlows.of(payload.postAuthenticationFlows))
            .maximumAuthenticationAge(Duration.parse(payload.maxAuthenticationAge))
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.valueOf(payload.authenticationResultReusePolicy))
            .messageSigningPolicy(MessageSigningPolicy.valueOf(payload.messageSigningPolicy))
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.valueOf(payload.requestSignatureValidationPolicy))
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.valueOf(payload.encryptionFallbackPolicy))
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.valueOf(payload.nameIdEncryptionPolicy))
            .assertionTimeCondition(AssertionTimeCondition.valueOf(payload.assertionTimeCondition))
            .assertionLifetime(Duration.parse(payload.assertionLifetime))
            .assertionSigningPolicy(AssertionSigningPolicy.valueOf(payload.assertionSigningPolicy))
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.valueOf(payload.assertionEncryptionPolicy))
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.valueOf(payload.attributeEncryptionPolicy))
            .maximumSPSessionLifetime(Duration.parse(payload.maximumSpSessionLifetime))
            .endpointValidationPolicy(EndpointValidationPolicy.valueOf(payload.endpointValidationPolicy))
            .attributeStatementPolicy(AttributeStatementPolicy.valueOf(payload.attributeStatementPolicy))
            .friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.valueOf(payload.friendlyNameRandomizationPolicy))
            .nameIdFormatPrecedence(NameIdentifiers.of(payload.nameIdFormatPrecedence))
            .requestSigningRequirement(RequestSigningRequirement.valueOf(payload.requestSigningRequirement))
            .build();
    }

    private static ProfilesPayload.Saml2Ecp toPayload(Saml2EcpProfileConfiguration profile) {

        ProfilesPayload.Saml2Ecp payload = new ProfilesPayload.Saml2Ecp();
        payload.status = profile.getStatus().name();
        payload.inboundFlows = profile.getInboundFlows().getFlows();
        payload.outboundFlows = profile.getOutboundFlows().getFlows();
        payload.messageSigningPolicy = profile.getMessageSigningPolicy().name();
        payload.requestSignatureValidationPolicy = profile.getRequestSignatureValidationPolicy().name();
        payload.encryptionFallbackPolicy = profile.getEncryptionFallbackPolicy().name();
        payload.nameIdEncryptionPolicy = profile.getNameIdEncryptionPolicy().name();
        payload.assertionTimeCondition = profile.getAssertionTimeCondition().name();
        payload.assertionLifetime = profile.getAssertionLifetime().toString();
        payload.assertionSigningPolicy = profile.getAssertionSigningPolicy().name();
        payload.authenticationResultReusePolicy = profile.getAuthenticationResultReusePolicy().name();
        payload.assertionEncryptionPolicy = profile.getAssertionEncryptionPolicy().name();
        payload.attributeEncryptionPolicy = profile.getAttributeEncryptionPolicy().name();
        payload.maximumSpSessionLifetime = profile.getMaximumSPSessionLifetime().toString();
        payload.endpointValidationPolicy = profile.getEndpointValidationPolicy().name();
        payload.attributeStatementPolicy = profile.getAttributeStatementPolicy().name();
        payload.friendlyNameRandomizationPolicy = profile.getFriendlyNameRandomizationPolicy().name();
        payload.nameIdFormatPrecedence = profile.getNameIdFormatPrecedence().getNameIdentifiers();
        payload.requestSigningRequirement = profile.getRequestSigningRequirement().name();
        return payload;
    }

    private static Result<Saml2EcpProfileConfiguration> toSaml2Ecp(ProfilesPayload.Saml2Ecp payload) {

        return Saml2EcpProfileConfiguration.from(SamlProfileConfigurationDefaults.saml2Ecp())
            .status(ProfileStatus.valueOf(payload.status))
            .inboundFlows(InterceptorFlows.of(payload.inboundFlows))
            .outboundFlows(InterceptorFlows.of(payload.outboundFlows))
            .messageSigningPolicy(MessageSigningPolicy.valueOf(payload.messageSigningPolicy))
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.valueOf(payload.requestSignatureValidationPolicy))
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.valueOf(payload.encryptionFallbackPolicy))
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.valueOf(payload.nameIdEncryptionPolicy))
            .assertionTimeCondition(AssertionTimeCondition.valueOf(payload.assertionTimeCondition))
            .assertionLifetime(Duration.parse(payload.assertionLifetime))
            .assertionSigningPolicy(AssertionSigningPolicy.valueOf(payload.assertionSigningPolicy))
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.valueOf(payload.authenticationResultReusePolicy))
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.valueOf(payload.assertionEncryptionPolicy))
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.valueOf(payload.attributeEncryptionPolicy))
            .maximumSPSessionLifetime(Duration.parse(payload.maximumSpSessionLifetime))
            .endpointValidationPolicy(EndpointValidationPolicy.valueOf(payload.endpointValidationPolicy))
            .attributeStatementPolicy(AttributeStatementPolicy.valueOf(payload.attributeStatementPolicy))
            .friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.valueOf(payload.friendlyNameRandomizationPolicy))
            .nameIdFormatPrecedence(NameIdentifiers.of(payload.nameIdFormatPrecedence))
            .requestSigningRequirement(RequestSigningRequirement.valueOf(payload.requestSigningRequirement))
            .build();
    }

    private static ProfilesPayload.Saml2AttributeQuery toPayload(Saml2AttributeQueryProfileConfiguration profile) {

        ProfilesPayload.Saml2AttributeQuery payload = new ProfilesPayload.Saml2AttributeQuery();
        payload.status = profile.getStatus().name();
        payload.inboundFlows = profile.getInboundFlows().getFlows();
        payload.outboundFlows = profile.getOutboundFlows().getFlows();
        payload.messageSigningPolicy = profile.getMessageSigningPolicy().name();
        payload.assertionTimeCondition = profile.getAssertionTimeCondition().name();
        payload.assertionLifetime = profile.getAssertionLifetime().toString();
        payload.assertionSigningPolicy = profile.getAssertionSigningPolicy().name();
        payload.requestSignatureValidationPolicy = profile.getRequestSignatureValidationPolicy().name();
        payload.encryptionFallbackPolicy = profile.getEncryptionFallbackPolicy().name();
        payload.nameIdEncryptionPolicy = profile.getNameIdEncryptionPolicy().name();
        payload.assertionEncryptionPolicy = profile.getAssertionEncryptionPolicy().name();
        payload.attributeEncryptionPolicy = profile.getAttributeEncryptionPolicy().name();
        payload.friendlyNameRandomizationPolicy = profile.getFriendlyNameRandomizationPolicy().name();
        return payload;
    }

    private static Result<Saml2AttributeQueryProfileConfiguration> toSaml2AttributeQuery(
        ProfilesPayload.Saml2AttributeQuery payload) {

        return Saml2AttributeQueryProfileConfiguration.from(SamlProfileConfigurationDefaults.saml2AttributeQuery())
            .status(ProfileStatus.valueOf(payload.status))
            .inboundFlows(InterceptorFlows.of(payload.inboundFlows))
            .outboundFlows(InterceptorFlows.of(payload.outboundFlows))
            .messageSigningPolicy(MessageSigningPolicy.valueOf(payload.messageSigningPolicy))
            .assertionTimeCondition(AssertionTimeCondition.valueOf(payload.assertionTimeCondition))
            .assertionLifetime(Duration.parse(payload.assertionLifetime))
            .assertionSigningPolicy(AssertionSigningPolicy.valueOf(payload.assertionSigningPolicy))
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.valueOf(payload.requestSignatureValidationPolicy))
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.valueOf(payload.encryptionFallbackPolicy))
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.valueOf(payload.nameIdEncryptionPolicy))
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.valueOf(payload.assertionEncryptionPolicy))
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.valueOf(payload.attributeEncryptionPolicy))
            .friendlyRandomizationPolicy(FriendlyNameRandomizationPolicy.valueOf(payload.friendlyNameRandomizationPolicy))
            .build();
    }

    private static ProfilesPayload.Saml2ArtifactResolution toPayload(Saml2ArtifactResolutionProfileConfiguration profile) {

        ProfilesPayload.Saml2ArtifactResolution payload = new ProfilesPayload.Saml2ArtifactResolution();
        payload.status = profile.getStatus().name();
        payload.inboundFlows = profile.getInboundFlows().getFlows();
        payload.outboundFlows = profile.getOutboundFlows().getFlows();
        payload.messageSigningPolicy = profile.getMessageSigningPolicy().name();
        payload.requestSignatureValidationPolicy = profile.getRequestSignatureValidationPolicy().name();
        payload.encryptionFallbackPolicy = profile.getEncryptionFallbackPolicy().name();
        payload.nameIdEncryptionPolicy = profile.getNameIdEncryptionPolicy().name();
        payload.assertionEncryptionPolicy = profile.getAssertionEncryptionPolicy().name();
        payload.attributeEncryptionPolicy = profile.getAttributeEncryptionPolicy().name();
        payload.assertionSigningPolicy = profile.getAssertionSigningPolicy().name();
        return payload;
    }

    private static Result<Saml2ArtifactResolutionProfileConfiguration> toSaml2ArtifactResolution(
        ProfilesPayload.Saml2ArtifactResolution payload) {

        return Saml2ArtifactResolutionProfileConfiguration.from(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .status(ProfileStatus.valueOf(payload.status))
            .inboundFlows(InterceptorFlows.of(payload.inboundFlows))
            .outboundFlows(InterceptorFlows.of(payload.outboundFlows))
            .messageSigningPolicy(MessageSigningPolicy.valueOf(payload.messageSigningPolicy))
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.valueOf(payload.requestSignatureValidationPolicy))
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.valueOf(payload.encryptionFallbackPolicy))
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.valueOf(payload.nameIdEncryptionPolicy))
            .assertionSigningPolicy(AssertionSigningPolicy.valueOf(payload.assertionSigningPolicy))
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.valueOf(payload.assertionEncryptionPolicy))
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.valueOf(payload.attributeEncryptionPolicy))
            .build();
    }

    private static ProfilesPayload.Saml2Logout toPayload(Saml2LogoutProfileConfiguration profile) {

        ProfilesPayload.Saml2Logout payload = new ProfilesPayload.Saml2Logout();
        payload.status = profile.getStatus().name();
        payload.inboundFlows = profile.getInboundFlows().getFlows();
        payload.outboundFlows = profile.getOutboundFlows().getFlows();
        payload.messageSigningPolicy = profile.getMessageSigningPolicy().name();
        payload.requestSignatureValidationPolicy = profile.getRequestSignatureValidationPolicy().name();
        payload.encryptionFallbackPolicy = profile.getEncryptionFallbackPolicy().name();
        payload.nameIdEncryptionPolicy = profile.getNameIdEncryptionPolicy().name();
        return payload;
    }

    private static Result<Saml2LogoutProfileConfiguration> toSaml2Logout(ProfilesPayload.Saml2Logout payload) {

        return Saml2LogoutProfileConfiguration.from(SamlProfileConfigurationDefaults.saml2Logout())
            .status(ProfileStatus.valueOf(payload.status))
            .inboundFlows(InterceptorFlows.of(payload.inboundFlows))
            .outboundFlows(InterceptorFlows.of(payload.outboundFlows))
            .messageSigningPolicy(MessageSigningPolicy.valueOf(payload.messageSigningPolicy))
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.valueOf(payload.requestSignatureValidationPolicy))
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.valueOf(payload.encryptionFallbackPolicy))
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.valueOf(payload.nameIdEncryptionPolicy))
            .build();
    }
}
