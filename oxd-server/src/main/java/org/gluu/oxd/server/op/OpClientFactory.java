package org.gluu.oxd.server.op;

import org.gluu.oxauth.client.*;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.model.crypto.signature.RSAPublicKey;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.jws.RSASigner;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxd.rs.protect.resteasy.PatProvider;
import org.gluu.oxd.rs.protect.resteasy.ResourceRegistrar;
import org.gluu.oxd.rs.protect.resteasy.RptPreProcessInterceptor;
import org.gluu.oxd.rs.protect.resteasy.ServiceProvider;
import org.gluu.oxd.server.introspection.ClientFactory;
import org.gluu.oxd.server.service.PublicOpKeyService;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;

public interface OpClientFactory {
    public TokenClient createTokenClient(String url);

    public TokenClient createTokenClientWithUmaProtectionScope(String url);

    public UserInfoClient createUserInfoClient(String url);

    public RegisterClient createRegisterClient(String url);

    public OpenIdConfigurationClient createOpenIdConfigurationClient(String url) throws Exception;

    public AuthorizeClient createAuthorizeClient(String url);

    public ResourceRegistrar createResourceRegistrar(PatProvider patProvider, ServiceProvider serviceProvider);

    public JwkClient createJwkClient(String url);

    public RSASigner createRSASigner(SignatureAlgorithm signatureAlgorithm, RSAPublicKey rsaPublicKey);

    public RptPreProcessInterceptor createRptPreProcessInterceptor(ResourceRegistrar resourceRegistrar);

    public ClientFactory createClientFactory();

    public UmaClientFactory createUmaClientFactory();

    public ClientRequest createClientRequest(String uriTemplate, ClientExecutor executor) throws Exception;
}
