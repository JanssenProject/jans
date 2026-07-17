package io.jans.shibboleth.trust.config.profile.capabilities;

import java.time.Duration;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.NameIdentifiers;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;

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