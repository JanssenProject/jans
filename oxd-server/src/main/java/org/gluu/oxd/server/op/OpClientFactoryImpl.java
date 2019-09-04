package org.gluu.oxd.server.op;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.server.service.PublicOpKeyService;

public class OpClientFactoryImpl implements OpClientFactory {

    public OpClientFactoryImpl() {
    }

    public TokenClient createTokenClient(String url) {
        return new TokenClient(url);
    }

    public UserInfoClient createUserInfoClient(String url) {
        return new UserInfoClient(url);
    }

    public RegisterClient createRegisterClient(String url) {
        return new RegisterClient(url);
    }

    public OpenIdConfigurationClient createOpenIdConfigurationClient(String url) {
        return new OpenIdConfigurationClient(url);
    }

    public AuthorizeClient createAuthorizeClient(String url) {
        return new AuthorizeClient(url);
    }

    public Validator createValidator(Jwt idToken, OpenIdConfigurationResponse discoveryResponse, PublicOpKeyService keyService) {
        return new Validator(idToken, discoveryResponse, keyService);
    }

}
