package io.jans.shibboleth.model.config.profiles.capabilities;

import java.time.Duration;

import io.jans.shibboleth.model.config.profiles.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AttributeStatementPolicy;
import io.jans.shibboleth.model.config.profiles.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.model.config.profiles.common.EndpointValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdentifiers;
import io.jans.shibboleth.model.config.profiles.common.RequestSigningRequirement;

public interface Saml2SsoConfigurationCapable {

    public AuthenticationResultReusePolicy getAuthenticationResultReusePolicy();
    public AssertionEncryptionPolicy getAssertionEncryptionPolicy();
    public AttributeEncryptionPolicy getAttributeEncryptionPolicy();
    public Duration getMaximumSPSessionLifetime();
    public EndpointValidationPolicy getEndpointValidationPolicy();
    public AttributeStatementPolicy getAttributeStatementPolicy();
    public FriendlyNameRandomizationPolicy getFriendlyNameRandomizationPolicy();
    public NameIdentifiers getNameIdFormatPrecedence();
    public RequestSigningRequirement getRequestSigningRequirement();

}