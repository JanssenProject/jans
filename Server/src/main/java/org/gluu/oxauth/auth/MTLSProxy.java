package org.gluu.oxauth.auth;

import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.gluu.oxauth.interception.MTLSInterception;
import org.gluu.oxauth.interception.MTLSInterceptionInterface;
import org.gluu.oxauth.model.ref.AuthenticatorReference;
import org.gluu.oxauth.model.ref.ClientReference;

import javax.ejb.DependsOn;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@DependsOn("appInitializer")
@Named
public class MTLSProxy implements MTLSInterceptionInterface {

    @MTLSInterception
    public boolean processMTLS(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain,
                               ClientReference client, AuthenticatorReference authenticator, AbstractCryptoProvider cryptoProvider) {
        return false;
    }
}
