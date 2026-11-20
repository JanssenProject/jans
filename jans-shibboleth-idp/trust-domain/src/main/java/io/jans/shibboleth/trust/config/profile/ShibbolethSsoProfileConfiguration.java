package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.time.Duration;
import java.util.Objects;

public record ShibbolethSsoProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    AuthenticationConfigurationSupport authenticationConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    SamlAssertionConfigurationSupport samlAssertionConfigurationSupport,
    AttributeStatementPolicy attributeStatementPolicy,
    NameIdentifiers nameIdFormatPrecedence)
    implements CommonConfigurationCapable, AuthenticationConfigurationCapable, SamlConfigurationCapable,
        SamlAssertionConfigurationCapable {

    public ShibbolethSsoProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(authenticationConfigurationSupport, "authenticationConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(samlAssertionConfigurationSupport, "samlAssertionConfigurationSupport");
        Objects.requireNonNull(attributeStatementPolicy, "attributeStatementPolicy");
        Objects.requireNonNull(nameIdFormatPrecedence, "nameIdFormatPrecedence");
    }

    //Profile Configuration
    @Override
    public ProfileType getType() {

        return ProfileType.SHIBBOLETH_SSO;

    }

    //saml assertion configuration 
    @Override
    public AssertionTimeCondition getAssertionTimeCondition () {

        return samlAssertionConfigurationSupport.assertionTimeCondition();
    }

    public AttributeStatementPolicy getAttributeStatementPolicy() {

        return attributeStatementPolicy;
    }

    public NameIdentifiers getNameIdFormatPrecedence() {

        return nameIdFormatPrecedence;
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

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            auth = config != null ? AuthenticationConfigurationSupport.from(config.authenticationConfigurationSupport()) : AuthenticationConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
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

        public Result<ShibbolethSsoProfileConfiguration> build() {

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

            Result<SamlAssertionConfigurationSupport> samlAssertionResult = samlAssertion.build();
            if (samlAssertionResult.isFailure()) {

                return Result.failure(samlAssertionResult.getError());
            }

            if (attributeStatementPolicy == null) {

                return Result.failure(
                    RequiredValueMissing.forField(ShibbolethSsoProfileConfiguration.class, "attributeStatementPolicy"));
            }

            if (nameIdFormatPrecedence == null) {

                return Result.failure(
                    RequiredValueMissing.forField(ShibbolethSsoProfileConfiguration.class, "nameIdFormatPrecedence"));
            }

            return Result.success(new ShibbolethSsoProfileConfiguration(
                commonResult.getValue(),authResult.getValue(),samlResult.getValue(),
                samlAssertionResult.getValue(), attributeStatementPolicy,nameIdFormatPrecedence
            ));
        }

    }
}