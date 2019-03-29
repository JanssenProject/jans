package org.xdi.oxauth.interception;

import org.gluu.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.ref.AuthenticatorReference;
import org.xdi.oxauth.model.ref.ClientReference;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface MTLSInterceptionInterface {
    boolean processMTLS(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain,
                        ClientReference client, AuthenticatorReference authenticator, AbstractCryptoProvider cryptoProvider) throws Exception;
}
