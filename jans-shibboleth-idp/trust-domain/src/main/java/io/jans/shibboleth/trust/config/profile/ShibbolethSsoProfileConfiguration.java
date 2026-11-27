package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

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

    public static class Builder extends Steps<Builder> {

        public Builder(ShibbolethSsoProfileConfiguration config) {

            super(config);
        }
    }

    private static class Steps<B extends Steps<B>>
        implements CommonBuilderSteps<B>, AuthenticationBuilderSteps<B>, SamlBuilderSteps<B>,
        SamlAssertionBuilderSteps<B> {

        private final CommonConfigurationSupport.Builder common;
        private final AuthenticationConfigurationSupport.Builder auth;
        private final SamlConfigurationSupport.Builder saml;
        private final SamlAssertionConfigurationSupport.Builder samlAssertion;
        private AttributeStatementPolicy attributeStatementPolicy;
        private NameIdentifiers nameIdFormatPrecedence;

        private Steps(ShibbolethSsoProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            auth = config != null ? AuthenticationConfigurationSupport.from(config.authenticationConfigurationSupport()) : AuthenticationConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            samlAssertion = config != null ? SamlAssertionConfigurationSupport.from(config.samlAssertionConfigurationSupport()) : SamlAssertionConfigurationSupport.builder();
            attributeStatementPolicy = config != null ? config.attributeStatementPolicy : null;
            nameIdFormatPrecedence = config != null ? config.nameIdFormatPrecedence : null;
        }

        @Override
        public CommonConfigurationSupport.Builder commonBuilder() {

            return common;
        }

        @Override
        public AuthenticationConfigurationSupport.Builder authenticationBuilder() {

            return auth;
        }

        @Override
        public SamlConfigurationSupport.Builder samlBuilder() {

            return saml;
        }

        @Override
        public SamlAssertionConfigurationSupport.Builder samlAssertionBuilder() {

            return samlAssertion;
        }

        @SuppressWarnings("unchecked")
        protected B self() {

            return (B) this;
        }

        public B attributeStatementPolicy(AttributeStatementPolicy policy) {

            attributeStatementPolicy = policy;
            return self();
        }

        public B nameIdFormatPrecedence(NameIdentifiers nameIdentifiers) {

            nameIdFormatPrecedence = nameIdentifiers;
            return self();
        }

        public Result<ShibbolethSsoProfileConfiguration> build() {

            CommonConfigurationSupport commonValue;
            AuthenticationConfigurationSupport authValue;
            SamlConfigurationSupport samlValue;
            SamlAssertionConfigurationSupport samlAssertionValue;

            try {

                commonValue = Builders.require(common.build());
                authValue = Builders.require(auth.build());
                samlValue = Builders.require(saml.build());
                samlAssertionValue = Builders.require(samlAssertion.build());
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
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
                commonValue, authValue, samlValue,
                samlAssertionValue, attributeStatementPolicy, nameIdFormatPrecedence
            ));
        }
    }
}
