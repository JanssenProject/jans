package io.jans.shibboleth.trust.config.profile;

import java.time.Duration;

import org.assertj.core.api.AbstractAssert;

import io.jans.shibboleth.trust.config.profile.capabilities.AuthenticationConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2SsoConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlAssertionConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionTimeCondition;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;

import java.util.Objects;

public class ProfileConfigurationAssert extends AbstractAssert<ProfileConfigurationAssert,Object> {

    private final ProfileType profileType;

    private ProfileConfigurationAssert(Object actual,ProfileType profileType) {

        super(actual,ProfileConfigurationAssert.class);
        this.profileType = profileType;
    }

    public static ProfileConfigurationAssert assertThat(Object actual,ProfileType profileType) {

        return new ProfileConfigurationAssert(actual,profileType);
    }

    public ProfileConfigurationAssert isInactive() {

        isNotNull();

        hasProfileConfigurationCapabilities(CommonConfigurationCapable.class);
        CommonConfigurationCapable commonconfig = asCommonConfigurationCapable();
        if (commonconfig.getStatus() != ProfileStatus.INACTIVE ) {

            failWithMessage("Profile configuration status is  <%s>. Expected: <%s>",commonconfig.getStatus(),ProfileStatus.INACTIVE);
        }
        return this;
    }

    public ProfileConfigurationAssert isActive() {

        isNotNull();
        hasProfileConfigurationCapabilities(CommonConfigurationCapable.class);
        CommonConfigurationCapable commonconfig = asCommonConfigurationCapable();
        if (commonconfig.getStatus() != ProfileStatus.ACTIVE ) {
            failWithMessage("Profile configuration status is <%s>. Expected: <%s>",commonconfig.getStatus(),ProfileStatus.ACTIVE);
        }

        return this;
    }

    public ProfileConfigurationAssert usesDefaultConfiguration() {

        isNotNull();

        switch(profileType) {
            case SHIBBOLETH_SSO:
                usesShibbolethSsoDefaultConfiguration();
                break;
            case SAML2_ATTRIBUTE_QUERY:
                usesSaml2AttributeQueryDefaultConfiguration();
                break;
            case SAML2_ARTIFACT_RESOLUTION:
                usesSaml2ArtifactResolutionDefaultConfiguration();
                break;
            case SAML2_ECP:
                usesSaml2EcpDefaultConfiguration();
                break;
            case SAML2_LOGOUT:
                usesSaml2LogoutDefaultConfiguration();
                break;
            case SAML2_SSO:
                usesSaml2SsoDefaultConfiguration();
                break;
            default:
                failWithMessage("Test support for profile configuration <%s> is missing",profileType);
                break;
        }

        return this;
    }

    public ProfileConfigurationAssert usesShibbolethSsoDefaultConfiguration() {

        isNotNull();

        ShibbolethSsoProfileConfiguration profileconfig = (ShibbolethSsoProfileConfiguration) actual;

        isInactive();

        hasNoInboundInterceptorFlows();
        hasNoOutboundInterceptorFlows();

        hasNoPostAuthenticationFlows();
        hasAuthenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE);
        hasMaxAuthenticationAge(Duration.ofMinutes(0));

        hasMessageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY);

        hasAssertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE);
        hasAssertionLifetime(Duration.ofMinutes(5));
        hasAssertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS);

        if ( !Objects.equals(profileconfig.getAttributeStatementPolicy(), AttributeStatementPolicy.OMIT_ATTRIBUTE_STATEMENT) ) {

            failWithMessage("Attribute statement policy for profile is <%s>. Expected: <%s>",
                profileconfig.getAttributeStatementPolicy(),
                AttributeStatementPolicy.OMIT_ATTRIBUTE_STATEMENT);
        }

        if (profileconfig.getNameIdFormatPrecedence().hasSome()) {

            failWithMessage("Profile configuration has at least one NameIdFormat. Expected: 0");
        }
        return this;
    }

    public ProfileConfigurationAssert usesSaml2AttributeQueryDefaultConfiguration() {

        isNotNull();

        Saml2AttributeQueryProfileConfiguration profileconfig = (Saml2AttributeQueryProfileConfiguration) actual();

        isInactive();

        hasNoInboundInterceptorFlows();
        hasNoOutboundInterceptorFlows();

        hasMessageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY);

        hasAssertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE);
        hasAssertionLifetime(Duration.ofMinutes(5));
        hasAssertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS);

        hasRequestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE);
        hasEncryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT);
        hasNameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS);

        if (! Objects.equals(profileconfig.getAssertionEncryptionPolicy(),AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS) ) {

            failWithMessage("Assertion encryption policy for profile is <%s>. Expected: <%s>",
                profileconfig.getAssertionEncryptionPolicy(),AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS);
        }

        if (! Objects.equals(profileconfig.getAttributeEncryptionPolicy(),AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES) ) {

            failWithMessage("Attribute encryption policy for profile is <%s>. Expected: <%s>",
                profileconfig.getAttributeEncryptionPolicy(),AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES);
        }

        if (! Objects.equals(profileconfig.getFriendlyNameRandomizationPolicy(),FriendlyNameRandomizationPolicy.RANDOMIZED) ) {

            failWithMessage("FriendlyName randomization policy for profile is <%s>. Expected: <%s>",
                profileconfig.getFriendlyNameRandomizationPolicy(),FriendlyNameRandomizationPolicy.RANDOMIZED);
        }

        return this;
    }

    public ProfileConfigurationAssert usesSaml2ArtifactResolutionDefaultConfiguration() {

        isNotNull();

        Saml2ArtifactResolutionProfileConfiguration profileconfig = (Saml2ArtifactResolutionProfileConfiguration) actual;

        isInactive();

        hasNoInboundInterceptorFlows();
        hasNoOutboundInterceptorFlows();
        
        hasMessageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY);
        
        hasRequestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE);
        hasEncryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT);
        hasNameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS);

        if (! Objects.equals(profileconfig.getAssertionSigningPolicy(),AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS) ) {

            failWithMessage("Assertion signing policy for profile is <%s>. Expected: <%s>",
                profileconfig.getAssertionSigningPolicy(),AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS);
        }

        if (! Objects.equals(profileconfig.getAssertionEncryptionPolicy(),AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS) ) {

            failWithMessage("Assertion encryption policy for profile is <%s>. Expected: <%s>",
                profileconfig.getAssertionEncryptionPolicy(),AssertionEncryptionPolicy.DO_NOT_ENCRYPT_ASSERTIONS);
        }

        if (! Objects.equals(profileconfig.getAttributeEncryptionPolicy(),AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES) ) {

            failWithMessage("Attribute encryption policy for profile is <%s>. Expected: <%s>",
                profileconfig.getAttributeEncryptionPolicy(),AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES);
        }

        return this;
    }

    public ProfileConfigurationAssert usesSaml2EcpDefaultConfiguration() {


        isNotNull();
        Saml2EcpProfileConfiguration profileconfig = (Saml2EcpProfileConfiguration) actual;

        isInactive();

        hasNoInboundInterceptorFlows();
        hasNoOutboundInterceptorFlows();

        hasMessageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY);

        hasRequestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE);
        hasEncryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT);
        hasNameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS);

        hasAssertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE);
        hasAssertionLifetime(Duration.ofMinutes(5));
        hasAssertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS);

        hasAuthenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE);
        hasAssertionEncryptionPolicy(AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS);
        hasAttributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES);
        hasMaximumSPSessionLifetime(Duration.ofMinutes(0));
        hasEndpointValidationPolicy(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT);
        hasAttributeStatementPolicy(AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT);
        hasFriendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED);
        hasNoNameIdFormatPrecedence();
        hasRequestSigningRequirement(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS);

        return this;

    }

    public ProfileConfigurationAssert usesSaml2SsoDefaultConfiguration() {

        isNotNull();
        Saml2SsoProfileConfiguration profileconfig = (Saml2SsoProfileConfiguration) actual;

        isInactive();

        hasNoInboundInterceptorFlows();
        hasNoOutboundInterceptorFlows();

        hasNoPostAuthenticationFlows();
        hasAuthenticationResultReusePolicy(AuthenticationResultReusePolicy.ALLOW_REUSE);
        hasMaxAuthenticationAge(Duration.ofMinutes(0));

        hasMessageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY);

        hasRequestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE);
        hasEncryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT);
        hasNameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS);

        hasAssertionTimeCondition(AssertionTimeCondition.INCLUDE_NOT_BEFORE);
        hasAssertionLifetime(Duration.ofMinutes(5));
        hasAssertionSigningPolicy(AssertionSigningPolicy.DO_NOT_SIGN_ASSERTIONS);

        hasAssertionEncryptionPolicy(AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS);
        hasAttributeEncryptionPolicy(AttributeEncryptionPolicy.DO_NOT_ENCRYPT_ATTRIBUTES);
        hasMaximumSPSessionLifetime(Duration.ofMinutes(0));
        hasEndpointValidationPolicy(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT);
        hasAttributeStatementPolicy(AttributeStatementPolicy.INCLUDE_ATTRIBUTE_STATEMENT);
        hasFriendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED);
        hasNoNameIdFormatPrecedence();
        hasRequestSigningRequirement(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS);

        return this;

    }

    public ProfileConfigurationAssert usesSaml2LogoutDefaultConfiguration() {

        isNotNull();

        Saml2LogoutProfileConfiguration profileconfig = (Saml2LogoutProfileConfiguration) actual;

        isInactive();
        
        hasNoInboundInterceptorFlows();
        hasNoOutboundInterceptorFlows();

        hasMessageSigningPolicy(MessageSigningPolicy.SIGN_RESPONSES_ONLY);

        hasRequestSignatureValidationPolicy(RequestSignatureValidationPolicy.REQUIRE_VALID_SIGNATURE);
        hasEncryptionFallbackPolicy(EncryptionFallbackPolicy.FAIL_IF_CANNOT_ENCRYPT);
        hasNameIdEncryptionPolicy(NameIdEncryptionPolicy.DO_NOT_ENCRYPT_NAMEIDS);


        return this;
    }

    public ProfileConfigurationAssert hasProfileConfigurationCapabilities(Class<?> capabilities) {
        
        isNotNull();

        if ( hasProfileConfigurationCapabilitiesInternal(capabilities) == false ) {

            failWithMessage("Profile configuration does not have the capabilities in <%s>",capabilities);
        }

        return this;
    }

    public ProfileConfigurationAssert hasNoInboundInterceptorFlows() {

        isNotNull();
        hasProfileConfigurationCapabilities(CommonConfigurationCapable.class);
        CommonConfigurationCapable commonconfig = asCommonConfigurationCapable();
        if ( commonconfig.getInboundFlows().hasSome() ) {

            failWithMessage("Profile configuration has at least one inbound interceptor flow. Expected: 0");
        }
        return this;
    }

    public ProfileConfigurationAssert hasNoOutboundInterceptorFlows() {

        isNotNull();
        hasProfileConfigurationCapabilities(CommonConfigurationCapable.class);
        CommonConfigurationCapable commonconfig = asCommonConfigurationCapable();
        if ( commonconfig.getOutboundFlows().hasSome() ) {

            failWithMessage("Profile configuration has at least one outbound interceptor flow. Expected: 0");
        }
        return this;
    }

    public ProfileConfigurationAssert hasNoPostAuthenticationFlows() {

        isNotNull();
        hasProfileConfigurationCapabilities(AuthenticationConfigurationCapable.class);
        AuthenticationConfigurationCapable authconfig = asAuthenticationConfigurationCapable();
        if ( authconfig.getPostAuthenticationFlows().hasSome() ) {

            failWithMessage("Profile configuration has at least one post authn interceptor flow. Expected: 0");
        }
        return this;
    }

    public ProfileConfigurationAssert hasAuthenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

        isNotNull();

        if ( hasProfileConfigurationCapabilitiesInternal(AuthenticationConfigurationCapable.class) ) {
            AuthenticationConfigurationCapable authconfig = asAuthenticationConfigurationCapable();
            if ( !Objects.equals(authconfig.getAuthenticationResultReusePolicy(),policy) ) {

                failWithMessage("Authentication result reuse policy for profile was <%s>. Expected: <%s>",
                        authconfig.getAuthenticationResultReusePolicy(),policy);
            }
        }else if( hasProfileConfigurationCapabilitiesInternal(Saml2ConfigurationCapable.class) ) {
            Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();
            if ( !Objects.equals(saml2ssoconfig.getAuthenticationResultReusePolicy(),policy) ) {
                failWithMessage("Authentication result reuse policy for profile was <%s>. Expected: <%s>",
                    saml2ssoconfig.getAuthenticationResultReusePolicy(),policy
                );
            }
        }else {
            failWithMessage("Authentication result reuse policy unsupported for profile <%s>",profileType);
        }

        return this;
    }

    public ProfileConfigurationAssert hasMaxAuthenticationAge(Duration age) {

        isNotNull();

        hasProfileConfigurationCapabilities(AuthenticationConfigurationCapable.class);

        AuthenticationConfigurationCapable authconfig = asAuthenticationConfigurationCapable();
        if (! Objects.equals(authconfig.getMaxAuthenticationAge(), age) ) {
            failWithMessage("Max authentication age for profile was <%s>. Expected: <%s>",authconfig.getMaxAuthenticationAge(),age);
        }

        return this;
    }

    public ProfileConfigurationAssert hasMessageSigningPolicy(MessageSigningPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(SamlConfigurationCapable.class);

        SamlConfigurationCapable samlconfig = asSamlConfigurationCapable();

        if (! Objects.equals(samlconfig.getMessageSigningPolicy(),policy) ) {

            failWithMessage("Message signing policy for profile was <%s>. Expected: <%s>",samlconfig.getMessageSigningPolicy(),policy);
        }
        return this;
    }

    public ProfileConfigurationAssert hasAssertionSigningPolicy(AssertionSigningPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(SamlAssertionConfigurationCapable.class);

        SamlAssertionConfigurationCapable samlassertionconfig = asSamlAssertionConfigurationCapable();

        if (! Objects.equals(samlassertionconfig.getAssertionSigningPolicy(),policy) ) {

            failWithMessage("Assertion signing policy for profile was <%s>. Expected: <%s>",samlassertionconfig.getAssertionSigningPolicy(),policy);
        }

        return this;
    }

    public ProfileConfigurationAssert hasAssertionTimeCondition(AssertionTimeCondition condition) {

        isNotNull();

        hasProfileConfigurationCapabilities(SamlAssertionConfigurationCapable.class);

        SamlAssertionConfigurationCapable samlassertionconfig = asSamlAssertionConfigurationCapable();

        if (! Objects.equals(samlassertionconfig.getAssertionTimeCondition(),condition) ) {

            failWithMessage("Assertion time condition for profile was <%s>. Expected: <%s>",samlassertionconfig.getAssertionTimeCondition(),condition);
        }

        return this;
    }

    public ProfileConfigurationAssert hasAssertionLifetime(Duration assertionLifetime) {

        isNotNull();

        hasProfileConfigurationCapabilities(SamlAssertionConfigurationCapable.class);

        SamlAssertionConfigurationCapable samlassertionconfig = asSamlAssertionConfigurationCapable();

        if (! Objects.equals(samlassertionconfig.getAssertionLifetime(),assertionLifetime) ) {
            failWithMessage("Assertion lifetime for profile was <%s>. Expected: <%s>",samlassertionconfig.getAssertionLifetime(),assertionLifetime);
        }

        return this;
    }

    public ProfileConfigurationAssert hasRequestSignatureValidationPolicy(RequestSignatureValidationPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2ConfigurationCapable.class);

        Saml2ConfigurationCapable saml2config = asSaml2ConfigurationCapable();

        if (! Objects.equals(saml2config.getRequestSignatureValidationPolicy(),policy) ) {

            failWithMessage("Request signature validation policy for profile was <%s>. Expected: <%s>",saml2config.getRequestSignatureValidationPolicy(),policy);
        }

        return this;
    }

    public ProfileConfigurationAssert hasEncryptionFallbackPolicy(EncryptionFallbackPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2ConfigurationCapable.class);

        Saml2ConfigurationCapable saml2config = asSaml2ConfigurationCapable();

        if (! Objects.equals(saml2config.getEncryptionFallbackPolicy(), policy) ) {

            failWithMessage("Encryption fallback policy for profile was <%s>. Expected: <%s>",saml2config.getEncryptionFallbackPolicy(),policy);
        }
        return this;
    }

    public ProfileConfigurationAssert hasNameIdEncryptionPolicy(NameIdEncryptionPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2ConfigurationCapable.class);

        Saml2ConfigurationCapable saml2config  = asSaml2ConfigurationCapable();

        if (! Objects.equals(saml2config.getNameIdEncryptionPolicy(),policy) ) {

            failWithMessage("NameID encryption policy for profile was <%s>. Expected: <%s>",saml2config.getNameIdEncryptionPolicy(),policy);
        }
        return this;
    }

    public ProfileConfigurationAssert hasAssertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if (! Objects.equals( saml2ssoconfig.getAssertionEncryptionPolicy(),policy) ) {

            failWithMessage("Assertion encryption policy for profile was <%s>. Expected: <%s>",saml2ssoconfig.getAssertionEncryptionPolicy(),policy);
        }

        return this;
    }

    public ProfileConfigurationAssert hasAttributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if (! Objects.equals( saml2ssoconfig.getAttributeEncryptionPolicy(),policy) ) {

            failWithMessage("Attribute encryption policy for profile was <%s>. Expected: <%s>",saml2ssoconfig.getAttributeEncryptionPolicy(),policy);
        }

        return this;
    }

    public ProfileConfigurationAssert hasMaximumSPSessionLifetime(Duration expected) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if (! Objects.equals( saml2ssoconfig.getMaximumSPSessionLifetime(), expected) ) {

            failWithMessage("MaximumSPSessionLifetime for profile was <%s>. Expected: <%s>",saml2ssoconfig.getMaximumSPSessionLifetime(),expected);
        }

        return this;
    }

    public ProfileConfigurationAssert hasEndpointValidationPolicy(EndpointValidationPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if (! Objects.equals( saml2ssoconfig.getEndpointValidationPolicy(), policy) ) {

            failWithMessage("Endpoint validation policy for profile was <%s>. Expected: <%s>",saml2ssoconfig.getEndpointValidationPolicy(),policy);
        }

        return this;
    }

    public ProfileConfigurationAssert hasAttributeStatementPolicy(AttributeStatementPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if (! Objects.equals( saml2ssoconfig.getAttributeStatementPolicy(), policy) ) {

            failWithMessage("Attribute statement policy for profile was <%s>. Expected: <%s>",saml2ssoconfig.getAttributeStatementPolicy(),policy);
        }

        return this;
    }

    public ProfileConfigurationAssert hasFriendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if( !Objects.equals(saml2ssoconfig.getFriendlyNameRandomizationPolicy(),policy) ) {

            failWithMessage("FriendlyName randomization policy for profile was <%s>. Expected: <%s>",saml2ssoconfig.getFriendlyNameRandomizationPolicy(),policy);
        }

        return this;
    }

    public ProfileConfigurationAssert hasNoNameIdFormatPrecedence() {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if ( saml2ssoconfig.getNameIdFormatPrecedence().hasSome() ) {

            failWithMessage("One or more NameIdentifiers associated with profile");
        }

        return this;
    }

    public ProfileConfigurationAssert hasRequestSigningRequirement(RequestSigningRequirement requirement) {

        isNotNull();

        hasProfileConfigurationCapabilities(Saml2SsoConfigurationCapable.class);

        Saml2SsoConfigurationCapable saml2ssoconfig = asSaml2SsoConfigurationCapable();

        if(! Objects.equals(saml2ssoconfig.getRequestSigningRequirement(),requirement) ) {

            failWithMessage("Request signing requirement for policy was <%s>",requirement);
        }

        return this;
    }

    private boolean hasProfileConfigurationCapabilitiesInternal(Class<?> capabilities) {

        return capabilities.isInstance(actual);
    }

    private CommonConfigurationCapable asCommonConfigurationCapable() {

        return (CommonConfigurationCapable) actual;
    }

    private AuthenticationConfigurationCapable asAuthenticationConfigurationCapable() {
        
        return (AuthenticationConfigurationCapable) actual;
    }

    private SamlAssertionConfigurationCapable asSamlAssertionConfigurationCapable() {

        return (SamlAssertionConfigurationCapable) actual;
    }

    private SamlConfigurationCapable asSamlConfigurationCapable() {

        return (SamlConfigurationCapable) actual;
    }

    private Saml2ConfigurationCapable asSaml2ConfigurationCapable() {

        return (Saml2ConfigurationCapable) actual;
    }

    private Saml2SsoConfigurationCapable asSaml2SsoConfigurationCapable() {

        return (Saml2SsoConfigurationCapable) actual;
    }
    
}