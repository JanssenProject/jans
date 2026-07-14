package io.jans.shibboleth.trust.config.profile.support;

import java.util.Objects;

import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.shared.Result;

public class Saml2ConfigurationSupport {

    private final RequestSignatureValidationPolicy requestSignatureValidationPolicy;
    private final EncryptionFallbackPolicy encryptionFallbackPolicy;
    private final NameIdEncryptionPolicy nameIdEncryptionPolicy;

    private Saml2ConfigurationSupport(RequestSignatureValidationPolicy requestSignatureValidationPolicy, 
        EncryptionFallbackPolicy encryptionFallbackPolicy, NameIdEncryptionPolicy nameIdEncryptionPolicy ) {
        
        this.requestSignatureValidationPolicy = requestSignatureValidationPolicy;
        this.encryptionFallbackPolicy = encryptionFallbackPolicy;
        this.nameIdEncryptionPolicy = nameIdEncryptionPolicy;   
    }

    public static Result<Saml2ConfigurationSupport> of(RequestSignatureValidationPolicy requestSignatureValidationPolicy,
            EncryptionFallbackPolicy encryptionFallbackPolicy, NameIdEncryptionPolicy nameIdEncryptionPolicy ) {
        
        return builder()
                .requestSignatureValidationPolicy(requestSignatureValidationPolicy)
                .encryptionFallbackPolicy(encryptionFallbackPolicy)
                .nameIdEncryptionPolicy(nameIdEncryptionPolicy)
                .build();
    }

    public RequestSignatureValidationPolicy getRequestSignatureValidationPolicy() {

        return requestSignatureValidationPolicy;
    }

    
    public EncryptionFallbackPolicy getEncryptionFallbackPolicy() {

        return encryptionFallbackPolicy;
    }

    public NameIdEncryptionPolicy getNameIdEncryptionPolicy() {

        return nameIdEncryptionPolicy;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true; 

        if ( o == null || getClass() != o.getClass() ) return false;

        Saml2ConfigurationSupport other = (Saml2ConfigurationSupport) o;

        return requestSignatureValidationPolicy == other.requestSignatureValidationPolicy
            &&  encryptionFallbackPolicy == other.encryptionFallbackPolicy 
            && nameIdEncryptionPolicy == other.nameIdEncryptionPolicy;
    }

    @Override
    public int hashCode() {

        return Objects.hash(requestSignatureValidationPolicy,encryptionFallbackPolicy,nameIdEncryptionPolicy);
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

            requestSignatureValidationPolicy = base != null ? base.requestSignatureValidationPolicy : null;
            encryptionFallbackPolicy = base != null ? base.encryptionFallbackPolicy : null ;
            nameIdEncryptionPolicy = base != null ? base.nameIdEncryptionPolicy : null; 
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

                return Result.failure(CannotBeNullOrBlank.forField("requestSignatureValidationPolicy"));
            }

            if (encryptionFallbackPolicy == null) {

                return Result.failure(CannotBeNullOrBlank.forField("encryptionFallbackPolicy"));
            }

            if (nameIdEncryptionPolicy == null) {

                return Result.failure(CannotBeNullOrBlank.forField("nameIdEncryptionPolicy"));
            }

            return Result.success(new Saml2ConfigurationSupport(requestSignatureValidationPolicy, encryptionFallbackPolicy, nameIdEncryptionPolicy));
        }
    }
}