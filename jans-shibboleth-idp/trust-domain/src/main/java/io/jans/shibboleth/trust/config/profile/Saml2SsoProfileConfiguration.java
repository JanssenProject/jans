package io.jans.shibboleth.trust.config.profile;

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
import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;
import io.jans.shibboleth.trust.config.profile.support.AuthenticationConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.Result;

import java.time.Duration;
import java.util.Objects;

public record Saml2SsoProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    AuthenticationConfigurationSupport authenticationConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport,
    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
    Saml2SsoConfigurationSupport saml2SsoConfigurationSupport)
    implements CommonConfigurationCapable, AuthenticationConfigurationCapable, SamlConfigurationCapable,
        SamlAssertionConfigurationCapable, Saml2ConfigurationCapable, Saml2SsoConfigurationCapable {

    public Saml2SsoProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(authenticationConfigurationSupport, "authenticationConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
        Objects.requireNonNull(samlAssertionConfigurationSupport, "samlAssertionConfigurationSupport");
        Objects.requireNonNull(saml2SsoConfigurationSupport, "saml2SsoConfigurationSupport");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_SSO;
    }

    @Override
    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationConfigurationSupport.authenticationResultReusePolicy();
    }
    //End Authentication profile configuration 
    //End Saml configuration 

    @Override 
    public EncryptionFallbackPolicy getEncryptionFallbackPolicy() {

        return saml2ConfigurationSupport.encryptionFallbackPolicy();
    }

    @Override 
    public NameIdEncryptionPolicy getNameIdEncryptionPolicy() {

        return saml2ConfigurationSupport.nameIdEncryptionPolicy();
    }

    @Override 
    public RequestSigningRequirement getRequestSigningRequirement() {

        return saml2SsoConfigurationSupport.requestSigningRequirement();
    }
    //End Saml2Sso configuration

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2SsoProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder extends Steps<Builder> {

        public Builder(Saml2SsoProfileConfiguration config) {

            super(config);
        }
    }

    /**
     * Fluent assembly steps backing {@link Builder}.
     *
     * <p>This type deliberately lives as a private nested class so the type-safe fluent surface of the
     * builder does not inflate the public contract of {@link Builder}, and so that each class stays small
     * and specialized: {@link Builder} is a thin self-typed facade, while this class owns the state and
     * the per-policy setter steps. Every step returns the concrete builder type {@code B} so chaining and
     * assignments keep their original types.</p>
     */
    private static class Steps<B extends Steps<B>> {

        private final CommonConfigurationSupport.Builder common;
        private final AuthenticationConfigurationSupport.Builder auth;
        private final SamlConfigurationSupport.Builder saml;
        private final Saml2ConfigurationSupport.Builder saml2;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private final Saml2SsoConfigurationSupport.Builder saml2sso;

        private Steps(Saml2SsoProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            auth = config != null ? AuthenticationConfigurationSupport.from(config.authenticationConfigurationSupport()) : AuthenticationConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
            saml2sso = config != null ? Saml2SsoConfigurationSupport.from(config.saml2SsoConfigurationSupport()) : Saml2SsoConfigurationSupport.builder();
        }

        @SuppressWarnings("unchecked")
        protected B self() {

            return (B) this;
        }

        public B status(ProfileStatus status) {

            common.status(status);
            return self();
        }

        public B inboundFlows(InterceptorFlows flows) {

            common.inboundFlows(flows);
            return self();
        }

        public B outboundFlows(InterceptorFlows flows) {

            common.outboundFlows(flows);
            return self();
        }

        public B postAuthenticationFlows(InterceptorFlows flows) {

            auth.postAuthenticationFlows(flows);
            return self();
        }

        public B authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

            auth.authenticationResultReusePolicy(policy);
            saml2sso.authenticationResultReusePolicy(policy);

            return self();
        }

        public B maximumAuthenticationAge(Duration age) {

            auth.maximumAuthenticationAge(age);
            return self();
        }

        public B messageSigningPolicy(MessageSigningPolicy policy) {

            saml.messageSigningPolicy(policy);
            return self();
        }

        public B requestSignatureValidationPolicy(RequestSignatureValidationPolicy policy) {

            saml2.requestSignatureValidationPolicy(policy);
            return self();
        }

        public B encryptionFallbackPolicy(EncryptionFallbackPolicy policy) {

            saml2.encryptionFallbackPolicy(policy);
            return self();
        }

        public B nameIdEncryptionPolicy(NameIdEncryptionPolicy policy) {

            saml2.nameIdEncryptionPolicy(policy);
            return self();
        }

        public B assertionSigningPolicy(AssertionSigningPolicy policy) {

            samlAssertion.assertionSigningPolicy(policy);
            return self();
        }

        public B assertionTimeCondition(AssertionTimeCondition condition) {

            samlAssertion.assertionTimeCondition(condition);
            return self();
        }

        public B assertionLifetime(Duration lifetime) {

            samlAssertion.assertionLifetime(lifetime);
            return self();
        }
        
        public B assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            saml2sso.assertionEncryptionPolicy(policy);
            return self();
        }

        public B attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            saml2sso.attributeEncryptionPolicy(policy);
            return self();
        }

        public B maximumSPSessionLifetime(Duration lifetime) {

            saml2sso.maximumSPSessionLifetime(lifetime);
            return self();
        }

        public B endpointValidationPolicy(EndpointValidationPolicy policy) {

            saml2sso.endpointValidationPolicy(policy);
            return self();
        }

        public B attributeStatementPolicy(AttributeStatementPolicy policy) {

            saml2sso.attributeStatementPolicy(policy);
            return self();
        }

        public B friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

            saml2sso.friendlyNameRandomizationPolicy(policy);
            return self();
        }

        public B nameIdFormatPrecedence(NameIdentifiers nameIdentifiers) {

            saml2sso.nameIdFormatPrecedence(nameIdentifiers);
            return self();
        }

        public B requestSigningRequirement(RequestSigningRequirement requirement) {
            
            saml2sso.requestSigningRequirement(requirement);
            return self();
        }

        public Result<Saml2SsoProfileConfiguration> build() {
        
            Result<CommonConfigurationSupport> commonResult = common.build();
            if (commonResult.isFailure()) {

                return Result.failure(commonResult.getError());
            }

            Result<AuthenticationConfigurationSupport> authResult = auth.build();
            if (authResult.isFailure()) {

                return Result.failure(authResult.getError());
            }

            Result<SamlConfigurationSupport> samlResult = saml.build();
            if (samlResult.isFailure()) {

                return Result.failure(samlResult.getError());
            }

            Result<Saml2ConfigurationSupport> saml2Result = saml2.build();
            if(saml2Result.isFailure()) {

                return Result.failure(saml2Result.getError());
            }

            Result<SamlAssertionConfigurationSupport> samlAssertionResult = samlAssertion.build();
            if (samlAssertionResult.isFailure()) {

                return Result.failure(samlAssertionResult.getError());
            }

            Result<Saml2SsoConfigurationSupport> saml2ssoResult = saml2sso.build();
            if (saml2ssoResult.isFailure()) {

                return Result.failure(saml2ssoResult.getError());
            }

            return Result.success(new Saml2SsoProfileConfiguration(
                commonResult.getValue(),authResult.getValue(), samlResult.getValue(),
                saml2Result.getValue(), samlAssertionResult.getValue(), saml2ssoResult.getValue()
            ));
        }

    }
}