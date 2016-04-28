/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.model.Configuration;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.gluu.oxeleven.model.SignatureAlgorithmFamily;
import org.gluu.oxeleven.service.ConfigurationService;
import org.gluu.oxeleven.service.PKCS11Service;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Map;

import static org.gluu.oxeleven.model.GenerateKeyResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version April 27, 2016
 */
@Name("generateKeyRestService")
public class GenerateKeyRestServiceImpl implements GenerateKeyRestService {

    @Logger
    private Log log;

    @Override
    public Response generateKey(String sigAlg, Long expirationTime) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(sigAlg);

            if (signatureAlgorithm == null) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the server does not support the provided signatureAlgorithm parameter.");
            } else if (expirationTime == null) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the expiration time parameter is mandatory.");
            } else if (signatureAlgorithm == SignatureAlgorithm.NONE || signatureAlgorithm.getFamily().equals(SignatureAlgorithmFamily.HMAC)) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The provided signature algorithm parameter is not supported");
            } else {
                Configuration configuration = ConfigurationService.instance().getConfiguration();
                String pkcs11Pin = configuration.getPkcs11Pin();
                Map<String, String> pkcs11Config = configuration.getPkcs11Config();
                String dnName = configuration.getDnName();

                PKCS11Service pkcs11 = new PKCS11Service(pkcs11Pin, pkcs11Config);
                String alias = pkcs11.generateKey(dnName, signatureAlgorithm, expirationTime);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(KEY_TYPE, signatureAlgorithm.getFamily());
                jsonObject.put(KEY_ID, alias);
                jsonObject.put(KEY_USE, "sig");
                jsonObject.put(ALGORITHM, signatureAlgorithm.getName());
                if (SignatureAlgorithmFamily.EC.equals(signatureAlgorithm.getFamily())) {
                    jsonObject.put(CURVE, signatureAlgorithm.getCurve());
                }
                jsonObject.put(EXPIRATION_TIME, expirationTime);

                builder.entity(jsonObject.toString());
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
        } catch (InvalidKeyException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (SignatureException e) {
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
}
