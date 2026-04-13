package io.jans.shibboleth.model.profiles;


public final class Saml2AttributeQuery extends BaseProfileConfiguration {

    @Override
    public ProfileType getType() {

        return ProfileType.SAML2_ATTRIBUTE_QUERY;
    }
}