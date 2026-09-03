package io.jans.shibboleth.trust.config.profile;

import io.jans.shibboleth.trust.config.profile.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.Saml2ConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.capabilities.SamlConfigurationCapable;
import io.jans.shibboleth.trust.config.profile.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.trust.config.profile.common.InterceptorFlows;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.trust.config.profile.support.CommonConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.Saml2ConfigurationSupport;
import io.jans.shibboleth.trust.config.profile.support.SamlConfigurationSupport;
import io.jans.kernel.Result;

import java.util.Objects;

public record Saml2LogoutProfileConfiguration(
    CommonConfigurationSupport commonConfigurationSupport,
    SamlConfigurationSupport samlConfigurationSupport,
    Saml2ConfigurationSupport saml2ConfigurationSupport)
    implements CommonConfigurationCapable, SamlConfigurationCapable, Saml2ConfigurationCapable {

    public Saml2LogoutProfileConfiguration {

        Objects.requireNonNull(commonConfigurationSupport, "commonConfigurationSupport");
        Objects.requireNonNull(samlConfigurationSupport, "samlConfigurationSupport");
        Objects.requireNonNull(saml2ConfigurationSupport, "saml2ConfigurationSupport");
    }

    //Profile configuration 
    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_LOGOUT;
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
    //End Saml2 Configuration 

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(Saml2LogoutProfileConfiguration config) {

        return new Builder(config);
    }

    public static class Builder {

        private final CommonConfigurationSupport.Builder common;
        private final SamlConfigurationSupport.Builder saml;
        private final Saml2ConfigurationSupport.Builder saml2;

        public Builder(Saml2LogoutProfileConfiguration config) {

            common = config != null ? CommonConfigurationSupport.from(config.commonConfigurationSupport()) : CommonConfigurationSupport.builder();
            saml = config != null ? SamlConfigurationSupport.from(config.samlConfigurationSupport()) : SamlConfigurationSupport.builder();
            saml2 = config != null ? Saml2ConfigurationSupport.from(config.saml2ConfigurationSupport()) : Saml2ConfigurationSupport.builder();
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

        public Builder messageSigningPolicy(MessageSigningPolicy policy) {

            saml.messageSigningPolicy(policy);
            return this;
        }

        public Builder requestSignatureValidationPolicy(RequestSignatureValidationPolicy policy) {

            saml2.requestSignatureValidationPolicy(policy);
            return this;
        }

        public Builder encryptionFallbackPolicy(EncryptionFallbackPolicy policy) {

            saml2.encryptionFallbackPolicy(policy);
            return this;
        }

        public Builder nameIdEncryptionPolicy(NameIdEncryptionPolicy policy) {

            saml2.nameIdEncryptionPolicy(policy);
            return this;
        }

        public Result<Saml2LogoutProfileConfiguration> build() {

            Result<CommonConfigurationSupport> commonResult = common.build();
            if (commonResult.isFailure()) {

                return Result.failure(commonResult.getError());
            }

            Result<SamlConfigurationSupport> samlResult = saml.build();
            if (samlResult.isFailure()) {

                return Result.failure(samlResult.getError());
            }

            Result<Saml2ConfigurationSupport> saml2Result = saml2.build();
            if (saml2Result.isFailure()) {

                return Result.failure(saml2Result.getError());
            }

            return Result.success(new Saml2LogoutProfileConfiguration(
                commonResult.getValue(), samlResult.getValue(),saml2Result.getValue())
            );
        }
    }
}