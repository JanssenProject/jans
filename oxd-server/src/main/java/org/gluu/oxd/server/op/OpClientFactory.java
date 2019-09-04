package org.gluu.oxd.server.op;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.server.service.PublicOpKeyService;

public interface OpClientFactory {
    public TokenClient createTokenClient(String url);

    public UserInfoClient createUserInfoClient(String url);

    public RegisterClient createRegisterClient(String url);

    public OpenIdConfigurationClient createOpenIdConfigurationClient(String url);

    public AuthorizeClient createAuthorizeClient(String url);

    public Validator createValidator(Jwt idToken, OpenIdConfigurationResponse discoveryResponse, PublicOpKeyService keyService);
}
