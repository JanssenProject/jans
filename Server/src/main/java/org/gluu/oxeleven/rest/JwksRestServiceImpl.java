/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import org.codehaus.jettison.json.JSONException;
import org.gluu.oxeleven.model.Configuration;
import org.gluu.oxeleven.model.Jwks;
import org.gluu.oxeleven.model.Key;
import org.gluu.oxeleven.service.ConfigurationService;
import org.gluu.oxeleven.service.PKCS11Service;
import org.gluu.oxeleven.util.Base64Util;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import sun.security.ec.ECPublicKeyImpl;
import sun.security.rsa.RSAPublicKeyImpl;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
@Name("jwksRestService")
public class JwksRestServiceImpl implements JwksRestService {

    @Logger
    private Log log;

    @Override
    public Response sign(Jwks jwks) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            if (jwks == null || jwks.getKeys() == null || jwks.getKeys().isEmpty()) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the provided aliasList parameter is mandatory.");
            } else {
                Configuration configuration = ConfigurationService.instance().getConfiguration();
                String pkcs11Pin = configuration.getPkcs11Pin();
                Map<String, String> pkcs11Config = configuration.getPkcs11Config();

                PKCS11Service pkcs11 = new PKCS11Service(pkcs11Pin, pkcs11Config);
                Jwks response = getJSonResponse(pkcs11, jwks);
                builder.entity(response);
            }
        } catch (CertificateException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (KeyStoreException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (UnrecoverableEntryException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        }

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(false);
        cacheControl.setNoStore(true);
        builder.cacheControl(cacheControl);
        builder.header("Pragma", "no-cache");
        return builder.build();
    }

    private Jwks getJSonResponse(PKCS11Service pkcs11, Jwks jwks)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, JSONException {
        for (Iterator<Key> iterator = jwks.getKeys().iterator(); iterator.hasNext(); ) {
            Key key = iterator.next();
            String alias = key.getKid();
            PublicKey publicKey = pkcs11.getPublicKey(alias);
            if (publicKey != null) {
                publicKey.getAlgorithm();
                if (publicKey instanceof RSAPublicKeyImpl) {
                    RSAPublicKeyImpl rsaPublicKey = (RSAPublicKeyImpl) publicKey;
                    key.setN(Base64Util.base64UrlEncodeUnsignedBigInt(rsaPublicKey.getModulus()));
                    key.setE(Base64Util.base64UrlEncodeUnsignedBigInt(rsaPublicKey.getPublicExponent()));
                } else if (publicKey instanceof ECPublicKeyImpl) {
                    ECPublicKeyImpl ecPublicKey = (ECPublicKeyImpl) publicKey;
                    key.setX(Base64Util.base64UrlEncodeUnsignedBigInt(ecPublicKey.getW().getAffineX()));
                    key.setY(Base64Util.base64UrlEncodeUnsignedBigInt(ecPublicKey.getW().getAffineY()));
                }
            } else {
                iterator.remove();
            }
        }

        return jwks;
    }
}
