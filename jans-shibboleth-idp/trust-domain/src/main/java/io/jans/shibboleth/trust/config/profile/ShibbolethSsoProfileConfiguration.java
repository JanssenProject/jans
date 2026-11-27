package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.*;
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
        SamlAssertionConfigurationCapable, ProfileConfigurationAccess {

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
        extends AbstractBuilderSteps<B>
        implements CommonBuilderSteps<B>, AuthenticationBuilderSteps<B>, SamlBuilderSteps<B>,
        SamlAssertionBuilderSteps<B> {

        private AttributeStatementPolicy attributeStatementPolicy;
        private NameIdentifiers nameIdFormatPrecedence;

        private Steps(ShibbolethSsoProfileConfiguration config) {

            super(config);

            attributeStatementPolicy = config != null ? config.attributeStatementPolicy : null;
            nameIdFormatPrecedence = config != null ? config.nameIdFormatPrecedence : null;
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

            try {

                return Result.success(new ShibbolethSsoProfileConfiguration(
                    Builders.require(commonBuilder().build()),
                    Builders.require(authenticationBuilder().build()),
                    Builders.require(samlBuilder().build()),
                    Builders.require(samlAssertionBuilder().build()),
                    Builders.require(attributeStatementPolicy, ShibbolethSsoProfileConfiguration.class, "attributeStatementPolicy"),
                    Builders.require(nameIdFormatPrecedence, ShibbolethSsoProfileConfiguration.class, "nameIdFormatPrecedence")
                ));
            }
            catch (Builders.Failure failure) {

                return Result.failure(failure.error);
            }
        }
    }
}
