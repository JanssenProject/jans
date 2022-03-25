package io.jans.ca.server.op;

import io.jans.as.client.*;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jws.RSASigner;
import jakarta.ws.rs.client.Invocation.Builder;
import io.jans.ca.rs.protect.resteasy.PatProvider;
import io.jans.ca.rs.protect.resteasy.ResourceRegistrar;
import io.jans.ca.rs.protect.resteasy.RptPreProcessInterceptor;
import io.jans.ca.rs.protect.resteasy.ServiceProvider;
import io.jans.ca.server.introspection.ClientFactory;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;

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

    public Builder createClientRequest(String uriTemplate, ClientHttpEngine engine) throws Exception;
}