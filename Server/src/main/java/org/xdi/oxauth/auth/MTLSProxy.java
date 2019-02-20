package org.xdi.oxauth.auth;

import org.xdi.oxauth.interception.MTLSInterception;
import org.xdi.oxauth.interception.MTLSInterceptionInterface;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.ref.AuthenticatorReference;
import org.xdi.oxauth.model.ref.ClientReference;

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
