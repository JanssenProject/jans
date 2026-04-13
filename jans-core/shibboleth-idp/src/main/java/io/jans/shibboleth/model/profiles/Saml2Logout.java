package io.jans.shibboleth.model.profiles;

public final class Saml2Logout extends BaseProfileConfiguration {
    

    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_LOGOUT;
    }
}