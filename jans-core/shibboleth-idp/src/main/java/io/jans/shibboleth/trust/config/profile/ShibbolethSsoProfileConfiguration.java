package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.util.TrustResult;

import java.time.Duration;
import java.util.Objects;

public final class ShibbolethSsoProfileConfiguration implements CommonConfigurationCapable, AuthenticationConfigurationCapable, 
    SamlConfigurationCapable, SamlAssertionConfigurationCapable {
    
    private final CommonConfigurationSupport commonConfigurationSupport;
    private final AuthenticationConfigurationSupport authenticationConfigurationSupport;
    private final SamlConfigurationSupport samlConfigurationSupport;
    private final SamlAssertionConfigurationSupport samlAssertionConfigurationSupport;

    private final AttributeStatementPolicy attributeStatementPolicy;
    private final NameIdentifiers nameIdFormatPrecedence;

    private ShibbolethSsoProfileConfiguration(
        CommonConfigurationSupport commonConfigurationSupport,
        AuthenticationConfigurationSupport authenticationConfigurationSupport,
        SamlConfigurationSupport samlConfigurationSupport,
        SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
        AttributeStatementPolicy attributeStatementPolicy,
        NameIdentifiers nameIdFormatPrecedence) {
        
        this.commonConfigurationSupport = commonConfigurationSupport;
        this.authenticationConfigurationSupport = authenticationConfigurationSupport;
        this.samlConfigurationSupport = samlConfigurationSupport;
        this.samlAssertionConfigurationSupport = samlAssertionConfigurationSupport;
        this.attributeStatementPolicy = attributeStatementPolicy;
        this.nameIdFormatPrecedence = nameIdFormatPrecedence;
    }

    //Profile Configuration
    @Override
    public ProfileType getType() {

        return ProfileType.SHIBBOLETH_SSO;

    }

    @Override
    public ProfileStatus getStatus() {

        return commonConfigurationSupport.getStatus();
    }

    @Override
    public InterceptorFlows getInboundFlows() {

        return commonConfigurationSupport.getInboundFlows();
    }

    @Override
    public InterceptorFlows getOutboundFlows() {

        return commonConfigurationSupport.getOutboundFlows();
    }
    //End Profile Configuration

    //Authentication Configuration
    @Override
    public InterceptorFlows getPostAuthenticationFlows() {

        return authenticationConfigurationSupport.getPostAuthenticationFlows();
    }

    @Override
    public Duration getMaxAuthenticationAge() {

        return authenticationConfigurationSupport.getMaximumAuthenticationAge();
    }

    @Override
    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return authenticationConfigurationSupport.getAuthenticationResultReusePolicy();
    }

    // saml configuration 
    @Override
    public MessageSigningPolicy getMessageSigningPolicy() {

        return samlConfigurationSupport.getMessageSigningPolicy();
    }

    //saml assertion configuration 
    @Override
    public AssertionTimeCondition getAssertionTimeCondition () {

        return samlAssertionConfigurationSupport.getAssertionTimeCondition();
    }

    @Override
    public Duration getAssertionLifetime() {

        return samlAssertionConfigurationSupport.getAssertionLifetime();
    }

    @Override
    public AssertionSigningPolicy getAssertionSigningPolicy() {

        return samlAssertionConfigurationSupport.getAssertionSigningPolicy();
    }

    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return attributeStatementPolicy;
    }

    public NameIdentifiers getNameIdFormatPrecedence() {

        return nameIdFormatPrecedence;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass() ) return false;

        ShibbolethSsoProfileConfiguration other = (ShibbolethSsoProfileConfiguration) o;
        return Objects.equals(commonConfigurationSupport,other.commonConfigurationSupport)
            && Objects.equals(authenticationConfigurationSupport,other.authenticationConfigurationSupport)
            && Objects.equals(samlConfigurationSupport,other.samlConfigurationSupport)
            && Objects.equals(samlAssertionConfigurationSupport,other.samlAssertionConfigurationSupport)
            && attributeStatementPolicy == other.attributeStatementPolicy 
            && Objects.equals(nameIdFormatPrecedence,other.nameIdFormatPrecedence);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
            commonConfigurationSupport,authenticationConfigurationSupport,samlAssertionConfigurationSupport,
            samlAssertionConfigurationSupport,attributeStatementPolicy,nameIdFormatPrecedence
        );
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(ShibbolethSsoProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder {

        private final CommonConfigurationSupport.Builder common;
        private final AuthenticationConfigurationSupport.Builder auth;
        private final SamlConfigurationSupport.Builder saml;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private AttributeStatementPolicy attributeStatementPolicy;
        private NameIdentifiers nameIdFormatPrecedence;

        public Builder(ShibbolethSsoProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport) : CommonConfigurationSupport.builder();
            auth = config != null ? AuthenticationConfigurationSupport.from(config.authenticationConfigurationSupport) : AuthenticationConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport) : SamlConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport) : SamlAssertionConfigurationSupport.builder();
            attributeStatementPolicy = config != null ?  config.attributeStatementPolicy : null;
            nameIdFormatPrecedence = config != null ? config.nameIdFormatPrecedence : null;
        }

        public Builder status(ProfileStatus status) {

            common.status(status);
            return this;
        }

        public Builder inboundFlows(InterceptorFlows flows) {

            common.inboundFlows(flows);
            return this;
        }

        public Builder outboundFlows(InterceptorFlows flows) {

            common.outboundFlows(flows);
            return this;
        }

        public Builder postAuthenticationFlows(InterceptorFlows flows) {

            auth.postAuthenticationFlows(flows);
            return this;
        }

        public Builder authenticationResultReusePolicy(AuthenticationResultReusePolicy policy) {

            auth.authenticationResultReusePolicy(policy);
            return this;
        }

        public Builder maximumAuthenticationAge(Duration age) {

            auth.maximumAuthenticationAge(age);
            return this;
        }

        public Builder messageSigningPolicy(MessageSigningPolicy policy) {

            saml.messageSigningPolicy(policy);
            return this;
        }

        public Builder assertionSigningPolicy(AssertionSigningPolicy policy) {

            samlAssertion.assertionSigningPolicy(policy);
            return this;
        }

        public Builder assertionTimeCondition(AssertionTimeCondition condition) {

            samlAssertion.assertionTimeCondition(condition);
            return this;
        }

        public Builder assertionLifetime(Duration lifetime) {

            samlAssertion.assertionLifetime(lifetime);
            return this;
        }

        public Builder attributeStatementPolicy(AttributeStatementPolicy policy) {

            attributeStatementPolicy = policy;
            return this;
        }

        public Builder nameIdFormatPrecedence(NameIdentifiers nameIdentifiers) {

            nameIdFormatPrecedence = nameIdentifiers;
            return this;
        }

        public TrustResult<ShibbolethSsoProfileConfiguration> build() {


            TrustResult<CommonConfigurationSupport> commonResult = common.build();
            if (commonResult.isFailure()) {

                return TrustResult.failure(commonResult.getError());
            }

            TrustResult<AuthenticationConfigurationSupport> authResult = auth.build();
            if (authResult.isFailure()) {
                return TrustResult.failure(authResult.getError());
            }

            TrustResult<SamlConfigurationSupport> samlResult = saml.build();
            if (samlResult.isFailure()) {

                return TrustResult.failure(samlResult.getError());
            }

            TrustResult<SamlAssertionConfigurationSupport> samlAssertionResult = samlAssertion.build();
            if (samlAssertionResult.isFailure()) {

                return TrustResult.failure(samlAssertionResult.getError());
            }

            if (attributeStatementPolicy == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("attributeStatementPolicy"));
            }

            if (nameIdFormatPrecedence == null) {

                return TrustResult.failure(CannotBeNullOrBlank.forField("nameIdFormatPrecedence"));
            }

            return TrustResult.success(new ShibbolethSsoProfileConfiguration(
                commonResult.getValue(),authResult.getValue(),samlResult.getValue(),
                samlAssertionResult.getValue(), attributeStatementPolicy,nameIdFormatPrecedence
            ));
        }

    }
}