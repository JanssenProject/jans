/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import org.codehaus.jettison.json.JSONException;
import org.gluu.oxeleven.model.Configuration;
import org.gluu.oxeleven.model.JwksRequestParam;
import org.gluu.oxeleven.model.KeyRequestParam;
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
 * @version April 18, 2016
 */
@Name("jwksRestService")
public class JwksRestServiceImpl implements JwksRestService {

    @Logger
    private Log log;

    @Override
    public Response jwks(JwksRequestParam jwksRequestParam) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            if (jwksRequestParam == null || jwksRequestParam.getKeyRequestParams() == null || jwksRequestParam.getKeyRequestParams().isEmpty()) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the provided aliasList parameter is mandatory.");
            } else {
                Configuration configuration = ConfigurationService.instance().getConfiguration();
                String pkcs11Pin = configuration.getPkcs11Pin();
                Map<String, String> pkcs11Config = configuration.getPkcs11Config();

                PKCS11Service pkcs11 = new PKCS11Service(pkcs11Pin, pkcs11Config);
                JwksRequestParam response = getJSonResponse(pkcs11, jwksRequestParam);
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

    private JwksRequestParam getJSonResponse(PKCS11Service pkcs11, JwksRequestParam jwksRequestParam)
            throws UnrecoverableEntryException, NoSuchAlgorithmException, KeyStoreException, JSONException {
        for (Iterator<KeyRequestParam> iterator = jwksRequestParam.getKeyRequestParams().iterator(); iterator.hasNext(); ) {
            KeyRequestParam keyRequestParam = iterator.next();
            String alias = keyRequestParam.getKid();
            PublicKey publicKey = pkcs11.getPublicKey(alias);
            if (publicKey != null) {
                publicKey.getAlgorithm();
                if (publicKey instanceof RSAPublicKeyImpl) {
                    RSAPublicKeyImpl rsaPublicKey = (RSAPublicKeyImpl) publicKey;
                    keyRequestParam.setN(Base64Util.base64UrlEncodeUnsignedBigInt(rsaPublicKey.getModulus()));
                    keyRequestParam.setE(Base64Util.base64UrlEncodeUnsignedBigInt(rsaPublicKey.getPublicExponent()));
                } else if (publicKey instanceof ECPublicKeyImpl) {
                    ECPublicKeyImpl ecPublicKey = (ECPublicKeyImpl) publicKey;
                    keyRequestParam.setX(Base64Util.base64UrlEncodeUnsignedBigInt(ecPublicKey.getW().getAffineX()));
                    keyRequestParam.setY(Base64Util.base64UrlEncodeUnsignedBigInt(ecPublicKey.getW().getAffineY()));
                }
            } else {
                iterator.remove();
            }
        }

        return jwksRequestParam;
    }
}
