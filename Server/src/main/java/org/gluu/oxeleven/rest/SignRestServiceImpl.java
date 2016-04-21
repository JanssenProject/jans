/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import com.google.common.base.Strings;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.model.Configuration;
import org.gluu.oxeleven.model.SignRequestParam;
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

import static org.gluu.oxeleven.model.SignResponseParam.SIGNATURE;

/**
 * @author Javier Rojas Blum
 * @version April 19, 2016
 */
@Name("signRestService")
public class SignRestServiceImpl implements SignRestService {

    @Logger
    private Log log;

    @Override
    public Response sign(SignRequestParam signRequestParam) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(signRequestParam.getSignatureAlgorithm());

            if (Strings.isNullOrEmpty(signRequestParam.getSigningInput())) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the provided signingInput parameter is mandatory.");
            } else if (signatureAlgorithm == null) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the server does not support the provided signatureAlgorithm parameter.");
            } else if (signatureAlgorithm != SignatureAlgorithm.NONE
                    && SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())
                    && Strings.isNullOrEmpty(signRequestParam.getSharedSecret())) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the provided shared secret parameter is mandatory.");
            } else if (signatureAlgorithm != SignatureAlgorithm.NONE
                    && !SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily()) // EC or RSA
                    && Strings.isNullOrEmpty(signRequestParam.getAlias())) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity("The request asked for an operation that cannot be supported because the provided alias parameter is mandatory.");
            } else {
                Configuration configuration = ConfigurationService.instance().getConfiguration();
                String pkcs11Pin = configuration.getPkcs11Pin();
                Map<String, String> pkcs11Config = configuration.getPkcs11Config();

                PKCS11Service pkcs11 = new PKCS11Service(pkcs11Pin, pkcs11Config);
                String signature = pkcs11.getSignature(signRequestParam.getSigningInput().getBytes(),
                        signRequestParam.getAlias(), signRequestParam.getSharedSecret(), signatureAlgorithm);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(SIGNATURE, signature);

                builder.entity(jsonObject.toString());
            }
        } catch (InvalidKeyException e) {
            builder = Response.status(Response.Status.BAD_REQUEST);
            builder.entity("Invalid key, either the alias or signatureAlgorithm parameter is not valid.");
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
