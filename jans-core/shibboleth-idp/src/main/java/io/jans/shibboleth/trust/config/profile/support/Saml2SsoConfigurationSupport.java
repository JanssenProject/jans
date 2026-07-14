package io.jans.shibboleth.trust.config.profile.support;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.util.TrustResult;

import java.time.Duration;
import java.util.Objects;


public class Saml2SsoConfigurationSupport {
    
    private final AuthenticationResultReusePolicy authenticationReuseResultPolicy;
    private final AssertionEncryptionPolicy assertionEncryptionPolicy;
    private final AttributeEncryptionPolicy attributeEncryptionPolicy;
    private final Duration maximumSPSessionLifetime;
    private final EndpointValidationPolicy endpointValidationPolicy;
    private final AttributeStatementPolicy attributeStatementPolicy;
    private final FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy;
    private final NameIdentifiers nameIdFormatPrecedence;
    private final RequestSigningRequirement requestSigningRequirement;

    private Saml2SsoConfigurationSupport (
        AuthenticationResultReusePolicy authenticationResultReusePolicy,
        AssertionEncryptionPolicy assertionEncryptionPolicy,
        AttributeEncryptionPolicy attributeEncryptionPolicy,
        Duration maximumSPSessionLifetime,
        EndpointValidationPolicy endpointValidationPolicy,
        AttributeStatementPolicy attributeStatementPolicy,
        FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy,
        NameIdentifiers nameIdFormatPrecedence,
        RequestSigningRequirement requestSigningRequirement ) {

        
        this.authenticationReuseResultPolicy = authenticationResultReusePolicy;
        this.assertionEncryptionPolicy = assertionEncryptionPolicy;
        this.attributeEncryptionPolicy = attributeEncryptionPolicy;
        this.maximumSPSessionLifetime  = maximumSPSessionLifetime;
        this.endpointValidationPolicy  = endpointValidationPolicy;
        this.attributeStatementPolicy  = attributeStatementPolicy;
        this.friendlyNameRandomizationPolicy = friendlyNameRandomizationPolicy;
        this.nameIdFormatPrecedence = nameIdFormatPrecedence;
        this.requestSigningRequirement = requestSigningRequirement;
    }

    public static final TrustResult<Saml2SsoConfigurationSupport> of (
        AuthenticationResultReusePolicy authenticationReuseResultPolicy,
        AssertionEncryptionPolicy assertionEncryptionPolicy,
        AttributeEncryptionPolicy attributeEncryptionPolicy,
        Duration maximumSPSessionLifetime,
        EndpointValidationPolicy endpointValidationPolicy,
        AttributeStatementPolicy attributeStatementPolicy,
        FriendlyNameRandomizationPolicy friendlyNameRandomizationPolicy,
        NameIdentifiers nameIdFormatPrecedence,
        RequestSigningRequirement requestSigningRequirement) {
        
        
        return builder()
            .authenticationResultReusePolicy(authenticationReuseResultPolicy)
            .assertionEncryptionPolicy(assertionEncryptionPolicy)
            .attributeEncryptionPolicy(attributeEncryptionPolicy)
            .maximumSPSessionLifetime(maximumSPSessionLifetime)
            .endpointValidationPolicy(endpointValidationPolicy)
            .attributeStatementPolicy(attributeStatementPolicy)
            .friendlyNameRandomizationPolicy(friendlyNameRandomizationPolicy)
            .nameIdFormatPrecedence(nameIdFormatPrecedence)
            .requestSigningRequirement(requestSigningRequirement)
            .build();
    }

    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationReuseResultPolicy;
    }

    public AssertionEncryptionPolicy getAssertionEncryptionPolicy() {

        return assertionEncryptionPolicy;
    }   

    public AttributeEncryptionPolicy getAttributeEncryptionPolicy() {

        return attributeEncryptionPolicy;
    }

    public Duration getMaximumSPSessionLifetime() {

        return maximumSPSessionLifetime;
    }

    public EndpointValidationPolicy getEndpointValidationPolicy() {

        return endpointValidationPolicy;
    }

    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return attributeStatementPolicy;
    }

    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return friendlyNameRandomizationPolicy;
    }

    public NameIdentifiers getNameIdFormatPrecedence() {

        return nameIdFormatPrecedence;
    }

    public RequestSigningRequirement getRequestSigningRequirement() {

        return requestSigningRequirement;
    }
    
    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;

        Saml2SsoConfigurationSupport other =  (Saml2SsoConfigurationSupport) o;

        return authenticationReuseResultPolicy == other.authenticationReuseResultPolicy
            && assertionEncryptionPolicy == other.assertionEncryptionPolicy 
            && attributeEncryptionPolicy == other.attributeEncryptionPolicy 
            && Objects.equals(maximumSPSessionLifetime,other.maximumSPSessionLifetime)
            && endpointValidationPolicy == other.endpointValidationPolicy
            && attributeStatementPolicy == other.attributeStatementPolicy
            && friendlyNameRandomizationPolicy == other.friendlyNameRandomizationPolicy
            && Objects.equals(nameIdFormatPrecedence,other.nameIdFormatPrecedence)
            && requestSigningRequirement == other.requestSigningRequirement;
    }

    @Override
    public int hashCode() {

        return Objects.hash(
            authenticationReuseResultPolicy,assertionEncryptionPolicy,attributeEncryptionPolicy,
            maximumSPSessionLifetime,endpointValidationPolicy,attributeStatementPolicy,
            friendlyNameRandomizationPolicy,nameIdFormatPrecedence,requestSigningRequirement);
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

            this.authenticationResultReusePolicy = base != null ? base.authenticationReuseResultPolicy : null ;
            this.assertionEncryptionPolicy = base != null ? base.assertionEncryptionPolicy : null ;
            this.attributeEncryptionPolicy = base != null ? base.attributeEncryptionPolicy : null ;
            this.maximumSPSessionLifetime  = base != null ? base.maximumSPSessionLifetime  : null ;
            this.endpointValidationPolicy = base != null ? base.endpointValidationPolicy : null ;
            this.attributeStatementPolicy = base != null ? base.attributeStatementPolicy : null ; 
            this.friendlyNameRandomizationPolicy = base != null ? base.friendlyNameRandomizationPolicy : null ;
            this.nameIdFormatPrecedence = base != null ? base.nameIdFormatPrecedence : null ;
            this.requestSigningRequirement = base != null ? base.requestSigningRequirement : null;
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

        public TrustResult<Saml2SsoConfigurationSupport> build() {
            
            
            if (authenticationResultReusePolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("authenticationResultReusePolicy"));
            }

            if (assertionEncryptionPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("assertionEncryptionPolicy"));
            }

            if (attributeEncryptionPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("attributeEncryptionPolicy"));
            }

            if (maximumSPSessionLifetime == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("maximumSPSessionLifetime"));
            }

            if (endpointValidationPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("endpointValidationPolicy"));
            }

            if (attributeStatementPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("attributeStatementPolicy"));
            }

            if (friendlyNameRandomizationPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("friendlyNameRandomizationPolicy"));
            }

            if (nameIdFormatPrecedence == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("nameIdFormatPrecedence"));
            }

            if (requestSigningRequirement == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("requestSigningRequirement"));
            }

            return TrustResult.success(new Saml2SsoConfigurationSupport(
                authenticationResultReusePolicy, assertionEncryptionPolicy, attributeEncryptionPolicy, 
                maximumSPSessionLifetime, endpointValidationPolicy, attributeStatementPolicy, 
                friendlyNameRandomizationPolicy, nameIdFormatPrecedence, requestSigningRequirement)
            );
        }
    }
}