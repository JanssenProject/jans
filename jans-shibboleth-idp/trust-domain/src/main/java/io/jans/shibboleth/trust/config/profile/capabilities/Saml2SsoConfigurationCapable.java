package io.jans.shibboleth.trust.config.profile.capabilities;

import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.config.profile.support.Saml2SsoConfigurationSupport;

import java.time.Duration;

public interface Saml2SsoConfigurationCapable {

    Saml2SsoConfigurationSupport saml2SsoConfigurationSupport();

    /**
     * Also declared by {@link AuthenticationConfigurationCapable}. A profile implementing both must
     * say which support answers it — see {@code Saml2SsoProfileConfiguration}.
     */
    default AuthenticationResultReusePolicy getAuthenticationResultReusePolicy() {

        return saml2SsoConfigurationSupport().authenticationResultReusePolicy();
    }

    default AssertionEncryptionPolicy getAssertionEncryptionPolicy() {

        return saml2SsoConfigurationSupport().assertionEncryptionPolicy();
    }

    default AttributeEncryptionPolicy getAttributeEncryptionPolicy() {

        return saml2SsoConfigurationSupport().attributeEncryptionPolicy();
    }

    default Duration getMaximumSPSessionLifetime() {

        return saml2SsoConfigurationSupport().maximumSPSessionLifetime();
    }

    default EndpointValidationPolicy getEndpointValidationPolicy() {

        return saml2SsoConfigurationSupport().endpointValidationPolicy();
    }

    default AttributeStatementPolicy getAttributeStatementPolicy() {

        return saml2SsoConfigurationSupport().attributeStatementPolicy();
    }

    default FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy() {

        return saml2SsoConfigurationSupport().friendlyNameRandomizationPolicy();
    }

    default NameIdentifiers getNameIdFormatPrecedence() {

        return saml2SsoConfigurationSupport().nameIdFormatPrecedence();
    }

    default RequestSigningRequirement getRequestSigningRequirement() {

        return saml2SsoConfigurationSupport().requestSigningRequirement();
    }
}
