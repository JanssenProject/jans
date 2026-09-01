package io.jans.shibboleth.trust.config.profile.support;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.time.Duration;
import java.util.Objects;


public record Saml2SsoConfigurationSupport(
    AuthenticationResultReusePolicy authenticationResultReusePolicy,
    AssertionEncryptionPolicy assertionEncryptionPolicy,
    AttributeEncryptionPolicy attributeEncryptionPolicy,
    Duration maximumSPSessionLifetime,
    EndpointValidationPolicy endpointValidationPolicy,
    AttributeStatementPolicy attributeStatementPolicy,
    FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy,
    NameIdentifiers nameIdFormatPrecedence,
    RequestSigningRequirement requestSigningRequirement) {

    public Saml2SsoConfigurationSupport {

        Objects.requireNonNull(authenticationResultReusePolicy, "authenticationResultReusePolicy");
        Objects.requireNonNull(assertionEncryptionPolicy, "assertionEncryptionPolicy");
        Objects.requireNonNull(attributeEncryptionPolicy, "attributeEncryptionPolicy");
        Objects.requireNonNull(maximumSPSessionLifetime, "maximumSPSessionLifetime");
        Objects.requireNonNull(endpointValidationPolicy, "endpointValidationPolicy");
        Objects.requireNonNull(attributeStatementPolicy, "attributeStatementPolicy");
        Objects.requireNonNull(friendlyNameRandomizationPolicy, "friendlyNameRandomizationPolicy");
        Objects.requireNonNull(nameIdFormatPrecedence, "nameIdFormatPrecedence");
        Objects.requireNonNull(requestSigningRequirement, "requestSigningRequirement");
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2SsoConfigurationSupport base) {

        return new Builder(base);
    }

    public static class Builder {

        private AuthenticationResultReusePolicy authenticationResultReusePolicy;
        private AssertionEncryptionPolicy assertionEncryptionPolicy;
        private AttributeEncryptionPolicy attributeEncryptionPolicy;
        private Duration maximumSPSessionLifetime;
        private EndpointValidationPolicy endpointValidationPolicy;
        private AttributeStatementPolicy attributeStatementPolicy;
        private FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;
        private NameIdentifiers nameIdFormatPrecedence;
        private RequestSigningRequirement requestSigningRequirement;

        public Builder(Saml2SsoConfigurationSupport base) {

            this.authenticationResultReusePolicy = base != null ? base.authenticationResultReusePolicy() : null ;
            this.assertionEncryptionPolicy = base != null ? base.assertionEncryptionPolicy() : null ;
            this.attributeEncryptionPolicy = base != null ? base.attributeEncryptionPolicy() : null ;
            this.maximumSPSessionLifetime  = base != null ? base.maximumSPSessionLifetime()  : null ;
            this.endpointValidationPolicy = base != null ? base.endpointValidationPolicy() : null ;
            this.attributeStatementPolicy = base != null ? base.attributeStatementPolicy() : null ; 
            this.friendlyNameRandomizationPolicy = base != null ? base.friendlyNameRandomizationPolicy() : null ;
            this.nameIdFormatPrecedence = base != null ? base.nameIdFormatPrecedence() : null ;
            this.requestSigningRequirement = base != null ? base.requestSigningRequirement() : null;
        }

        public Builder authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

            authenticationResultReusePolicy = policy;
            return this;
        }

        public Builder assertionEncryptionPolicy(AssertionEncryptionPolicy policy) {

            assertionEncryptionPolicy = policy;
            return this;
        }

        public Builder attributeEncryptionPolicy(AttributeEncryptionPolicy policy) {

            attributeEncryptionPolicy = policy;
            return this;
        }

        public Builder maximumSPSessionLifetime(Duration lifetime) {

            maximumSPSessionLifetime = lifetime;
            return this;
        }

        public Builder endpointValidationPolicy(EndpointValidationPolicy policy) {

            endpointValidationPolicy = policy;
            return this;
        }

        public Builder attributeStatementPolicy(AttributeStatementPolicy policy) {

            attributeStatementPolicy = policy;
            return this;
        }

        public Builder friendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy policy) {

            friendlyNameRandomizationPolicy = policy;
            return this;
        }

        public Builder nameIdFormatPrecedence(NameIdentifiers nameIdentifiers) {

            nameIdFormatPrecedence = nameIdentifiers;
            return this;
        }

        public Builder requestSigningRequirement(RequestSigningRequirement requirement) {

            requestSigningRequirement = requirement;
            return this;
        }

        public Result<Saml2SsoConfigurationSupport> build() {
            
            
            if (authenticationResultReusePolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2SsoConfigurationSupport.class, "authenticationResultReusePolicy"));
            }

            if (assertionEncryptionPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "assertionEncryptionPolicy"));
            }

            if (attributeEncryptionPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "attributeEncryptionPolicy"));
            }

            if (maximumSPSessionLifetime == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "maximumSPSessionLifetime"));
            }

            if (endpointValidationPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "endpointValidationPolicy"));
            }

            if (attributeStatementPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "attributeStatementPolicy"));
            }

            if (friendlyNameRandomizationPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(
                        Saml2SsoConfigurationSupport.class, "friendlyNameRandomizationPolicy"));
            }

            if (nameIdFormatPrecedence == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "nameIdFormatPrecedence"));
            }

            if (requestSigningRequirement == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2SsoConfigurationSupport.class, "requestSigningRequirement"));
            }

            return Result.success(new Saml2SsoConfigurationSupport(
                authenticationResultReusePolicy, assertionEncryptionPolicy, attributeEncryptionPolicy, 
                maximumSPSessionLifetime, endpointValidationPolicy, attributeStatementPolicy, 
                friendlyNameRandomizationPolicy, nameIdFormatPrecedence, requestSigningRequirement)
            );
        }
    }
}