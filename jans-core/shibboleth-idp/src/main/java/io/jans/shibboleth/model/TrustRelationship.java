package io.jans.shibboleth.model;

import io.jans.shibboleth.model.config.profiles.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionTimeCondition;
import io.jans.shibboleth.model.config.profiles.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AttributeStatementPolicy;
import io.jans.shibboleth.model.config.profiles.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.model.config.profiles.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.model.config.profiles.common.EndpointValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.model.config.profiles.common.InterceptorFlows;
import io.jans.shibboleth.model.config.profiles.common.MessageSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdentifiers;
import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.config.profiles.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.RequestSigningRequirement;
import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.*;
import io.jans.shibboleth.model.util.TrustResult;

import java.time.Duration;
import java.util.Objects;


public class TrustRelationship {

    private static final int INITIAL_VERSION = 1;

    private Id id;
    private DisplayName displayName;
    private Description description;
    private final TrustNature nature;

    private int version;
    private TrustStatus status;
    private MetadataSource metadataSource;

    private EntityIds discoveredEntityIds;

    private ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration;
    private Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration;
    private Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration;
    private Saml2EcpProfileConfiguration saml2EcpProfileConfiguration;
    private Saml2SsoProfileConfiguration saml2SsoProfileConfiguration;
    private Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration;

    private TrustRelationship(Id id, DisplayName displayName, Description description, 
        TrustNature nature, int version, TrustStatus status, MetadataSource metadataSource, EntityIds discoveredEntityIds,
        ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration,
        Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration,
        Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration,
        Saml2EcpProfileConfiguration saml2EcpProfileConfiguration,
        Saml2SsoProfileConfiguration saml2SsoProfileConfiguration,
        Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration ) {
        
        
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.nature = nature;

        this.version = version;
        this.status  = status;
        this.metadataSource = metadataSource;
        this.discoveredEntityIds =  discoveredEntityIds;

        this.shibbolethSsoProfileConfiguration = shibbolethSsoProfileConfiguration;
        this.saml2ArtifactResolutionProfileConfiguration = saml2ArtifactResolutionProfileConfiguration;
        this.saml2AttributeQueryProfileConfiguration = saml2AttributeQueryProfileConfiguration; 
        this.saml2EcpProfileConfiguration = saml2EcpProfileConfiguration;
        this.saml2SsoProfileConfiguration = saml2SsoProfileConfiguration;
        this.saml2LogoutProfileConfiguration = saml2LogoutProfileConfiguration;
    }

    private int incrementVersion() {

        version++;
        return version;
    }

    public Id getId() {

        return id;
    }

    public DisplayName getDisplayName() {

        return displayName;
    }

    public TrustResult<Void> updateDisplayName(DisplayName newDisplayName) {

        if (newDisplayName == null) {

            return TrustResult.failure(DisplayNameError.required());
        }
        displayName = newDisplayName;
        incrementVersion();
        return TrustResult.success(null);
    } 

    public Description getDescription() {

        return description;
    }

    public TrustResult<Void> updateDescription(Description newDescription) {

        description = newDescription != null ? newDescription : Description.of("");
        incrementVersion();
        return TrustResult.success(null);

    }

    public TrustNature getNature() {

        return nature;
    }

    public boolean isAggregateNature() {

        return nature == TrustNature.AGGREGATE;
    }

    public boolean isIndividualNature() {

        return nature == TrustNature.INDIVIDUAL;
    }


    public TrustStatus getStatus() {

        return status;
    }

    public int getVersion() {

        return version;
    }

    public MetadataSource getMetadataSource() {

        return metadataSource;
    }

    private boolean isMetadataSourceAllowedForNature(MetadataSource source) {

        boolean allowed = false;

        switch(nature) {
            case INDIVIDUAL:
                allowed = isMetadataSourceAllowedForIndividualNature(source);
                break;
            case AGGREGATE:
                allowed = isMetadataSourceAllowedForAggregateNature(source);
                break;
        }

        return allowed;
    }

    private boolean isMetadataSourceNotAllowedForNature(MetadataSource source) {

        return !isMetadataSourceAllowedForNature(source);
    }

    private boolean isMetadataSourceAllowedForIndividualNature(MetadataSource source) {

        boolean allowed = false;

        switch(source.getType()) {
            case FILE:
            case URI:
            case UPSTREAM:
            case MANUAL:
            case NONE:
                allowed = true;
                break;
            case MDQ:
                allowed = false;
                break;
        }

        return allowed;
    }

    private boolean isMetadataSourceAllowedForAggregateNature(MetadataSource source) {

        boolean allowed = false;
        switch(source.getType()) {
            case FILE:
            case URI:
            case MDQ:
            case NONE:
                allowed = true;
                break;
            case UPSTREAM:
            case MANUAL:
                allowed = false;
                break;
        }
        return allowed;
    }

    public TrustResult<Void> updateMetadataSource(MetadataSource source) {

        if ( source == null ) {

            return TrustResult.failure(new CannotBeNullOrBlank("source"));
        }

        if ( isMetadataSourceNotAllowedForNature(source) ) {

            final String operation = String.format("updateMetadataSource(%s)",source.getType());
            final TrustNature requiredNature = nature == TrustNature.INDIVIDUAL ? TrustNature.AGGREGATE : TrustNature.INDIVIDUAL;
            return TrustResult.failure(new OperationRestrictedToNature(operation, requiredNature, nature));
        }

        if ( source.getType() == MetadataSourceType.NONE && status == TrustStatus.DRAFT ) {

            return TrustResult.success(null);
        }

        incrementVersion();
        return TrustResult.success(null);
    }

    public EntityIds getDiscoveredEntityIds() {

        return discoveredEntityIds;
    }

    public TrustResult<Void> incorporateDiscoveredEntityIds(EntityIds discoveredEntityIds) {


        if ( discoveredEntityIds == null ) {

            return TrustResult.failure(new CannotBeNullOrBlank("discoveredEntityIds"));
        }

        if ( !isAggregateNature() ) {

            return TrustResult.failure(new OperationRestrictedToNature("incorporateDiscoveredEntityIds", TrustNature.AGGREGATE, nature));
        }

        if (!status.isActivating()) {

            return TrustResult.failure(new InvalidStatusForOperation(status, "incorporateDiscoveredEntityIds"));
        }
        incrementVersion();
        return null;
    }

    public boolean hasNoMetadataSource() {

        return Objects.equals(metadataSource,NoMetadataSource.getInstance());
    }

    public boolean hasAnyDiscoveredEntityIds() {

        return discoveredEntityIds.hasAny();
    }

    public boolean hasNoDiscoveredEntityIds() {

        return discoveredEntityIds.hasNone();
    }

    public ShibbolethSsoProfileConfiguration getShibbolethSsoProfileConfiguration() {

        return shibbolethSsoProfileConfiguration;
    }

    public Saml2AttributeQueryProfileConfiguration getSaml2AttributeQueryProfileConfiguration() {

        return saml2AttributeQueryProfileConfiguration;
    }

    public Saml2ArtifactResolutionProfileConfiguration getSaml2ArtifactResolutionProfileConfiguration() {

        return saml2ArtifactResolutionProfileConfiguration;
    }

    public Saml2EcpProfileConfiguration getSaml2EcpProfileConfiguration() {

        return saml2EcpProfileConfiguration;
    }

    public Saml2SsoProfileConfiguration getSaml2SsoProfileConfiguration() {
        
        return saml2SsoProfileConfiguration;
    }

    public Saml2LogoutProfileConfiguration getSaml2LogoutProfileConfiguration() {

        return saml2LogoutProfileConfiguration;
    }

    public static TrustResult<TrustRelationship> create (
        final String displayName,
        final String description,
        final TrustNature nature
    )

    {

        
        Id id = Id.unassigned();

        TrustResult<DisplayName> displayNameResult = DisplayName.of(displayName);
        if( displayNameResult.isFailure() ) {
            return TrustResult.failure(displayNameResult.getError());
        }
        final DisplayName displayNameValue = displayNameResult.getValue();

        Description descriptionValue = Description.of(description);
        
        if ( nature == null ) {

            return TrustResult.failure(TrustNatureError.required());
        }

        int version =  INITIAL_VERSION;
        TrustStatus status = TrustStatus.DRAFT;
        MetadataSource metadataSource = new NoMetadataSource();
        EntityIds discoveredEntityIds = EntityIds.empty();
        
        ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration = defaultShibbolethSsoProfileConfiguration();
        Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration = defaultSaml2ArtifactResolutionProfileConfiguration();
        Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration = defaultSaml2AttributeQueryProfileConfiguration();
        Saml2EcpProfileConfiguration saml2EcpProfileConfiguration = defaultSaml2EcpProfileConfiguration();
        Saml2SsoProfileConfiguration saml2SsoProfileConfiguration = defaultSaml2SsoProfileConfiguration();
        Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration = defaultSaml2LogoutProfileConfiguration();

        return TrustResult.success(new TrustRelationship(
            id,displayNameValue,descriptionValue,nature,version,
            status,metadataSource,discoveredEntityIds,
            shibbolethSsoProfileConfiguration,saml2ArtifactResolutionProfileConfiguration,
            saml2AttributeQueryProfileConfiguration,saml2EcpProfileConfiguration,
            saml2SsoProfileConfiguration,saml2LogoutProfileConfiguration
        ));
    }

    private static ShibbolethSsoProfileConfiguration defaultShibbolethSsoProfileConfiguration() {

        return ShibbolethSsoProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .postAuthenticationFlows(InterceptorFlows.empty())
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE)
            .maximumAuthenticationAge(Duration.ofMinutes(0))
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .attributeStatementPolicy(AttributeStatementPolicy.OMIT_ATTRIBUTE_STATEMENT)
            .nameIdFormatPrecedence(NameIdentifiers.empty())
            .build()
            .getValue();
    }

    private static Saml2AttributeQueryProfileConfiguration defaultSaml2AttributeQueryProfileConfiguration() {

        return Saml2AttributeQueryProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .friendlyRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED)

            .build()
            .getValue();
    }

    private static Saml2ArtifactResolutionProfileConfiguration defaultSaml2ArtifactResolutionProfileConfiguration() {

        return Saml2ArtifactResolutionProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .build()
            .getValue();
    }

    private static Saml2EcpProfileConfiguration defaultSaml2EcpProfileConfiguration() {

        return Saml2EcpProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .maximumSPSessionLifetime(Duration.ofMinutes(0))
            .endpointValidationPolicy(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT)
            .attributeStatementPolicy((AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT))
            .friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED)
            .nameIdFormatPrecedence(NameIdentifiers.empty())
            .requestSigningRequirement(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS)
            .build()
            .getValue();
    }

    private static Saml2SsoProfileConfiguration defaultSaml2SsoProfileConfiguration() {

        return Saml2SsoProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .postAuthenticationFlows(InterceptorFlows.empty())
            .authenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE)
            .maximumAuthenticationAge(Duration.ofMinutes(0))
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .assertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE)
            .assertionLifetime(Duration.ofMinutes(5))
            .assertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS)
            .assertionEncryptionPolicy(AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS)
            .attributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES)
            .maximumSPSessionLifetime(Duration.ofMinutes(0))
            .endpointValidationPolicy(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT)
            .attributeStatementPolicy(AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT)
            .friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED)
            .nameIdFormatPrecedence(NameIdentifiers.empty())
            .requestSigningRequirement(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS)
            .build()
            .getValue();
    }

    private static Saml2LogoutProfileConfiguration defaultSaml2LogoutProfileConfiguration() {

        return Saml2LogoutProfileConfiguration.builder()
            .status(ProfileStatus.INACTIVE)
            .inboundFlows(InterceptorFlows.empty())
            .outboundFlows(InterceptorFlows.empty())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY)
            .requestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE)
            .encryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT)
            .nameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS)
            .build()
            .getValue();
    }
}