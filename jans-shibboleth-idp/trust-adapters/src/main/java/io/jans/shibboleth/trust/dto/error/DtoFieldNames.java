package io.jans.shibboleth.trust.dto.error;

import io.jans.kernel.DomainError;
import io.jans.kernel.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.EntityIds;
import io.jans.shibboleth.trust.config.ReleasedAttribute;
import io.jans.shibboleth.trust.config.ReleasedAttributes;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.metadata.FileMetadataSource;
import io.jans.shibboleth.trust.config.metadata.ManualMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MdqMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UriMetadataSource;
import io.jans.shibboleth.trust.config.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.trust.config.metadata.manual.SamlX509CertificateInfo;
import io.jans.shibboleth.trust.config.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.trust.config.profile.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationLogEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Translates a domain field into the request-body field clients know it by.
 *
 * <p>Most failures are labelled at the point of construction, where the mapper has both the DTO
 * field and the domain value in scope. This table covers the rest: errors raised <em>inside</em> a
 * domain builder or aggregate, which surface at the boundary naming a domain type and one of its
 * fields and nothing the client would recognise.
 *
 * <p>The mapping is declared rather than derived. A camel-to-snake convention would look plausible
 * and be wrong: the domain's {@code nameIdEncryptionPolicy} is {@code nameid_encryption_policy} on
 * the wire, {@code maximumSPSessionLifetime} is {@code maximum_sp_session_lifetime},
 * {@code maximumAuthenticationAge} is {@code max_authentication_age}, and the domain's
 * {@code friendNameRandomizationPolicy} is {@code friendly_name_randomization_policy}. A derived
 * name would emit fields that do not exist in the API.
 *
 * <p>An unmapped domain field resolves to {@link #UNRESOLVED} rather than leaking its domain name.
 */
public final class DtoFieldNames {

    /**
     * Returned when a domain field has no request-body counterpart. Callers report the failure
     * against the request as a whole rather than inventing a field name.
     */
    public static final String UNRESOLVED = "";

    private static final Map<String, String> FIELDS = fields();

    private DtoFieldNames() {
    }

    /**
     * The request-body field named by {@code error}, or {@link #UNRESOLVED} if it names none.
     */
    public static String resolve(DomainError error) {

        if (!(error instanceof RequiredValueMissing)) {

            return UNRESOLVED;
        }

        RequiredValueMissing missing = (RequiredValueMissing) error;
        return FIELDS.getOrDefault(key(missing.getOwner(), missing.getFieldName()), UNRESOLVED);
    }

    public static boolean isMapped(Class<?> owner, String domainField) {

        return FIELDS.containsKey(key(owner, domainField));
    }

    /**
     * Every request-body field this table can name. Exposed so a test can check each one really
     * exists on a DTO — a typo here would otherwise reach clients as a field they cannot find.
     */
    public static Collection<String> mappedDtoFields() {

        return FIELDS.values();
    }

    private static String key(Class<?> owner, String domainField) {

        return owner.getName() + "#" + domainField;
    }

    private static void put(Map<String, String> map, Class<?> owner, String domainField, String dtoField) {

        map.put(key(owner, domainField), dtoField);
    }

    /**
     * A single-field type: the type is the field, so the domain side has no field name.
     */
    private static void put(Map<String, String> map, Class<?> owner, String dtoField) {

        map.put(key(owner, ""), dtoField);
    }

    private static Map<String, String> fields() {

        Map<String, String> map = new HashMap<>();

        // single-field value objects — see RequiredValueMissing.of(Class)
        put(map, DisplayName.class, "display_name");
        put(map, EntityId.class, "entity_id");
        put(map, ValidityPeriod.class, "valid_until");
        put(map, SamlX509CertificateInfo.class, "signing_certificate");
        put(map, UriMetadataSource.class, "uri");
        put(map, MdqMetadataSource.class, "base_url");
        put(map, FileMetadataSource.class, "token");
        put(map, WorkerId.class, "origin");
        put(map, WorkItemId.class, "id");
        put(map, TrustRelationshipRef.class, "trust_relationship_ref");

        // the trust relationship aggregate
        put(map, TrustRelationship.class, "id", "id");
        put(map, TrustRelationship.class, "displayName", "display_name");
        put(map, TrustRelationship.class, "description", "description");
        put(map, TrustRelationship.class, "nature", "nature");
        put(map, TrustRelationship.class, "version", "version");
        put(map, TrustRelationship.class, "status", "status");
        put(map, TrustRelationship.class, "metadataSource", "metadata_source");
        put(map, TrustRelationship.class, "releasedAttributes", "released_attributes");
        put(map, TrustRelationship.class, "activationDiagnostics", "activation_diagnostics");
        put(map, TrustRelationship.class, "discoveredEntityIds", "discovered_entity_ids");
        put(map, TrustRelationship.class, "shibbolethSsoProfileConfiguration", "profiles.shibboleth_sso");
        put(map, TrustRelationship.class, "saml2SsoProfileConfiguration", "profiles.saml2_sso");
        put(map, TrustRelationship.class, "saml2ArtifactResolutionProfileConfiguration",
            "profiles.saml2_artifact_resolution");
        put(map, TrustRelationship.class, "saml2AttributeQueryProfileConfiguration",
            "profiles.saml2_attribute_query");
        put(map, TrustRelationship.class, "saml2EcpProfileConfiguration", "profiles.saml2_ecp");
        put(map, TrustRelationship.class, "saml2LogoutProfileConfiguration", "profiles.saml2_logout");

        // metadata sources
        put(map, ManualMetadataSource.class, "entityId", "entity_id");
        put(map, ManualMetadataSource.class, "validUntil", "valid_until");
        put(map, ManualMetadataSource.class, "assertionConsumerService", "assertion_consumer_service");
        put(map, ManualMetadataSource.class, "signingCertificate", "signing_certificate");
        put(map, UpstreamMetadataSource.class, "parentId", "parent_id");
        put(map, UpstreamMetadataSource.class, "entityId", "entity_id");
        put(map, AssertionConsumerService.class, "location", "location");
        put(map, AssertionConsumerService.class, "binding", "binding");

        // released attributes
        put(map, ReleasedAttribute.class, "id", "id");
        put(map, ReleasedAttribute.class, "displayName", "display_name");
        put(map, ReleasedAttributes.class, "attribute", "attributes");
        put(map, EntityIds.class, "id", "discovered_entity_ids");

        // profile configuration — the shared support types back several profiles, and every profile
        // request spells these fields the same way
        put(map, CommonConfigurationSupport.class, "status", "status");
        put(map, CommonConfigurationSupport.class, "inboundFlows", "inbound_flows");
        put(map, CommonConfigurationSupport.class, "outboundFlows", "outbound_flows");
        put(map, SamlConfigurationSupport.class, "messageSigningPolicy", "message_signing_policy");
        put(map, Saml2ConfigurationSupport.class, "requestSignatureValidationPolicy",
            "request_signature_validation_policy");
        put(map, Saml2ConfigurationSupport.class, "encryptionFallbackPolicy", "encryption_fallback_policy");
        put(map, Saml2ConfigurationSupport.class, "nameIdEncryptionPolicy", "nameid_encryption_policy");
        put(map, SamlAssertionConfigurationSupport.class, "assertionSigningPolicy", "assertion_signing_policy");
        put(map, SamlAssertionConfigurationSupport.class, "assertionTimeCondition", "assertion_time_condition");
        put(map, SamlAssertionConfigurationSupport.class, "assertionLifetime", "assertion_lifetime");
        put(map, AuthenticationConfigurationSupport.class, "postAuthenticationFlows", "post_authentication_flows");
        put(map, AuthenticationConfigurationSupport.class, "authenticationResultReusePolicy",
            "authentication_result_reuse_policy");
        put(map, AuthenticationConfigurationSupport.class, "maximumAuthenticationAge", "max_authentication_age");
        put(map, Saml2SsoConfigurationSupport.class, "authenticationResultReusePolicy",
            "authentication_result_reuse_policy");
        put(map, Saml2SsoConfigurationSupport.class, "assertionEncryptionPolicy", "assertion_encryption_policy");
        put(map, Saml2SsoConfigurationSupport.class, "attributeEncryptionPolicy", "attribute_encryption_policy");
        put(map, Saml2SsoConfigurationSupport.class, "maximumSPSessionLifetime", "maximum_sp_session_lifetime");
        put(map, Saml2SsoConfigurationSupport.class, "endpointValidationPolicy", "endpoint_validation_policy");
        put(map, Saml2SsoConfigurationSupport.class, "attributeStatementPolicy", "attribute_statement_policy");
        put(map, Saml2SsoConfigurationSupport.class, "friendlyNameRandomizationPolicy",
            "friendly_name_randomization_policy");
        put(map, Saml2SsoConfigurationSupport.class, "nameIdFormatPrecedence", "nameid_format_precedence");
        put(map, Saml2SsoConfigurationSupport.class, "requestSigningRequirement", "request_signing_requirement");
        put(map, ShibbolethSsoProfileConfiguration.class, "attributeStatementPolicy", "attribute_statement_policy");
        put(map, ShibbolethSsoProfileConfiguration.class, "nameIdFormatPrecedence", "nameid_format_precedence");
        put(map, Saml2ArtifactResolutionProfileConfiguration.class, "assertionSigningPolicy",
            "assertion_signing_policy");
        put(map, Saml2ArtifactResolutionProfileConfiguration.class, "assertionEncryptionPolicy",
            "assertion_encryption_policy");
        put(map, Saml2ArtifactResolutionProfileConfiguration.class, "attributeEncryptionPolicy",
            "attribute_encryption_policy");
        put(map, Saml2AttributeQueryProfileConfiguration.class, "assertionEncryptionPolicy",
            "assertion_encryption_policy");
        put(map, Saml2AttributeQueryProfileConfiguration.class, "attributeEncryptionPolicy",
            "attribute_encryption_policy");
        put(map, Saml2AttributeQueryProfileConfiguration.class, "friendNameRandomizationPolicy",
            "friendly_name_randomization_policy");

        // activation diagnostics
        put(map, ActivationDiagnostics.class, "status", "status");
        put(map, ActivationDiagnostics.class, "origin", "origin");
        put(map, ActivationDiagnostics.class, "logEntries", "log_entries");
        put(map, ActivationDiagnostics.class, "startedAt", "started_at");
        put(map, ActivationDiagnostics.class, "completedAt", "completed_at");
        put(map, ActivationLogEntry.class, "timestamp", "timestamp");
        put(map, ActivationLogEntry.class, "level", "level");
        put(map, ActivationLogEntry.class, "message", "message");

        return Collections.unmodifiableMap(map);
    }
}
