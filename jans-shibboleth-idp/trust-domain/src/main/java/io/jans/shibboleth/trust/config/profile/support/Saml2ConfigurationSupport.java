package io.jans.shibboleth.trust.config.profile.support;

import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

public record Saml2ConfigurationSupport(
    RequestSignatureValidationPolicy requestSignatureValidationPolicy,
    EncryptionFallbackPolicy encryptionFallbackPolicy,
    NameIdEncryptionPolicy nameIdEncryptionPolicy) {

    public Saml2ConfigurationSupport {

        Objects.requireNonNull(requestSignatureValidationPolicy, "requestSignatureValidationPolicy");
        Objects.requireNonNull(encryptionFallbackPolicy, "encryptionFallbackPolicy");
        Objects.requireNonNull(nameIdEncryptionPolicy, "nameIdEncryptionPolicy");
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2ConfigurationSupport base) {

        return new Builder(base);
    }

    public static class Builder {
        
        private RequestSignatureValidationPolicy requestSignatureValidationPolicy;
        private EncryptionFallbackPolicy encryptionFallbackPolicy;
        private NameIdEncryptionPolicy nameIdEncryptionPolicy;

        public Builder(Saml2ConfigurationSupport base) {

            requestSignatureValidationPolicy = base != null ? base.requestSignatureValidationPolicy() : null;
            encryptionFallbackPolicy = base != null ? base.encryptionFallbackPolicy() : null ;
            nameIdEncryptionPolicy = base != null ? base.nameIdEncryptionPolicy() : null; 
        } 

        public Builder requestSignatureValidationPolicy(RequestSignatureValidationPolicy policy) {

            this.requestSignatureValidationPolicy = policy;
            return this;
        }

        public Builder encryptionFallbackPolicy(EncryptionFallbackPolicy policy) {

            this.encryptionFallbackPolicy = policy;
            return this;
        }

        public Builder nameIdEncryptionPolicy(NameIdEncryptionPolicy policy) {

            this.nameIdEncryptionPolicy = policy;
            return this;
        }

        public Result<Saml2ConfigurationSupport> build() {

            if (requestSignatureValidationPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2ConfigurationSupport.class, "requestSignatureValidationPolicy"));
            }

            if (encryptionFallbackPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2ConfigurationSupport.class, "encryptionFallbackPolicy"));
            }

            if (nameIdEncryptionPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(Saml2ConfigurationSupport.class, "nameIdEncryptionPolicy"));
            }

            return Result.success(new Saml2ConfigurationSupport(requestSignatureValidationPolicy, encryptionFallbackPolicy, nameIdEncryptionPolicy));
        }
    }
}