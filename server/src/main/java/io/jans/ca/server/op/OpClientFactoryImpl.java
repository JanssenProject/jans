package io.jans.ca.server.op;

import io.jans.as.client.*;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.RSASigner;
import io.jans.ca.server.introspection.ClientFactory;
import io.jans.ca.rs.protect.resteasy.PatProvider;
import io.jans.ca.rs.protect.resteasy.ResourceRegistrar;
import io.jans.ca.rs.protect.resteasy.RptPreProcessInterceptor;
import io.jans.ca.rs.protect.resteasy.ServiceProvider;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;

public class OpClientFactoryImpl implements OpClientFactory {

    public OpClientFactoryImpl() {
    }

    public TokenClient createTokenClient(String url) {
        return new TokenClient(url);
    }

    public TokenClient createTokenClientWithUmaProtectionScope(String url) {
        return new TokenClient(url);
    }

    public UserInfoClient createUserInfoClient(String url) {
        return new UserInfoClient(url);
    }

    public RegisterClient createRegisterClient(String url) {
        return new RegisterClient(url);
    }

    public OpenIdConfigurationClient createOpenIdConfigurationClient(String url) throws Exception{
        return new OpenIdConfigurationClient(url);
    }

    public AuthorizeClient createAuthorizeClient(String url) {
        return new AuthorizeClient(url);
    }

    public ResourceRegistrar createResourceRegistrar(PatProvider patProvider, ServiceProvider serviceProvider) {
        return new ResourceRegistrar(patProvider, serviceProvider);
    }

    public ClientFactory createClientFactory() {
        return ClientFactory.instance();
    }

    public UmaClientFactory createUmaClientFactory() {
        return UmaClientFactory.instance();
    }

    public JwkClient createJwkClient(String url) {
        return new JwkClient(url);
    }

    public RSASigner createRSASigner(SignatureAlgorithm signatureAlgorithm, RSAPublicKey rsaPublicKey) {
        return new RSASigner(signatureAlgorithm, rsaPublicKey);
    }

    public RptPreProcessInterceptor createRptPreProcessInterceptor(ResourceRegistrar resourceRegistrar) {
        return new RptPreProcessInterceptor(resourceRegistrar);
    }

    public ClientRequest createClientRequest(String uriTemplate, ClientExecutor executor) throws Exception {
        return new ClientRequest(uriTemplate, executor);
    }

}
