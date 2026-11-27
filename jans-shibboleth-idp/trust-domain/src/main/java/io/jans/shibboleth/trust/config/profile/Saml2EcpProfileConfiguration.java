package io.jans.shibboleth.trust.config.profile;

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
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlAssertionConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.Result;

import java.time.Duration;
import java.util.Objects;

public record Saml2EcpProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport,
    Saml2SsoConfigurationSupport saml2SsoConfigurationSupport)
    implements CommonConfigurationCapable, SamlConfigurationCapable, SamlAssertionConfigurationCapable,
        Saml2ConfigurationCapable, Saml2SsoConfigurationCapable {

    public Saml2EcpProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(samlAssertionConfigurationSupport, "samlAssertionConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
        Objects.requireNonNull(saml2SsoConfigurationSupport, "saml2SsoConfigurationSupport");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ECP;
    }
    //End Profile Configuration 
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

    public static Builder from(Saml2EcpProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder extends Steps<Builder> {

        public Builder(Saml2EcpProfileConfiguration config) {

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
        private final SamlConfigurationSupport.Builder saml;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private final Saml2ConfigurationSupport.Builder saml2;
        private final Saml2SsoConfigurationSupport.Builder saml2Sso;

        private Steps(Saml2EcpProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            saml  = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();
            saml2Sso = config != null ? Saml2SsoConfigurationSupport.from(config.saml2SsoConfigurationSupport()) : Saml2SsoConfigurationSupport.builder();
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

        public B messageSigningPolicy(MessageSigningPolicy policy) {

            saml.messageSigningPolicy(policy);
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

        public B authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

            saml2Sso.authenticationResultReusePolicy(policy);
            return self();
        }

        public B assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            saml2Sso.assertionEncryptionPolicy(policy);
            return self();
        }

        public B attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            saml2Sso.attributeEncryptionPolicy(policy);
            return self();
        }

        public B maximumSPSessionLifetime(Duration lifetime) {

            saml2Sso.maximumSPSessionLifetime(lifetime);
            return self();
        }

        public B endpointValidationPolicy(EndpointValidationPolicy policy) {

            saml2Sso.endpointValidationPolicy(policy);
            return self();
        }

        public B attributeStatementPolicy(AttributeStatementPolicy policy) {

            saml2Sso.attributeStatementPolicy(policy);
            return self();
        }

        public B friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

            saml2Sso.friendlyNameRandomizationPolicy(policy);
            return self();
        }

        public B nameIdFormatPrecedence(NameIdentifiers nameIdentifiers) {

            saml2Sso.nameIdFormatPrecedence(nameIdentifiers);
            return self();
        }

        public B requestSigningRequirement(RequestSigningRequirement requirement) {

            saml2Sso.requestSigningRequirement(requirement);
            return self();
        }

        public Result<Saml2EcpProfileConfiguration> build() {

            
            Result<CommonConfigurationSupport> commonResult = common.build();
            if (commonResult.isFailure()) {

                return Result.failure(commonResult.getError());
            }

            Result<SamlConfigurationSupport> samlResult = saml.build();
            if (samlResult.isFailure()) {

                return Result.failure(samlResult.getError());
            }

            Result<SamlAssertionConfigurationSupport> samlAssertionResult = samlAssertion.build();
            if (samlAssertionResult.isFailure()) {

                return Result.failure(samlAssertionResult.getError());
            }

            Result<Saml2ConfigurationSupport> saml2Result = saml2.build();

            if (saml2Result.isFailure()) {

                return Result.failure(saml2Result.getError());
            }

            Result<Saml2SsoConfigurationSupport> saml2SsoResult = saml2Sso.build();

            if (saml2SsoResult.isFailure()) {

                return Result.failure(saml2SsoResult.getError());
            }

            return Result.success(new Saml2EcpProfileConfiguration(
                commonResult.getValue(),samlResult.getValue(),samlAssertionResult.getValue(),
                saml2Result.getValue(),saml2SsoResult.getValue()
            ));
        }
    }
}