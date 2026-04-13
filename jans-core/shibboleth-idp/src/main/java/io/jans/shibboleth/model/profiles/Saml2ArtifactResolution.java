package io.jans.shibboleth.model.profiles;

public final class Saml2ArtifactResolution extends BaseProfileConfiguration {

    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ATTRIBUTE_RESOLUTION;
    }
}