/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import static org.gluu.oxeleven.model.VerifySignatureResponseParam.VERIFIED;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxeleven.model.SignatureAlgorithm;
import org.gluu.oxeleven.model.SignatureAlgorithmFamily;
import org.gluu.oxeleven.model.VerifySignatureRequestParam;
import org.gluu.oxeleven.service.PKCS11Service;
import org.gluu.oxeleven.util.StringUtils;
import org.slf4j.Logger;

import com.google.common.base.Strings;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
@Path("/")
public class VerifySignatureRestServiceImpl implements VerifySignatureRestService {

	@Inject
	private Logger log;

	@Inject
	private PKCS11Service pkcs11Service;

    public Response verifySignature(VerifySignatureRequestParam verifySignatureRequestParam) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(verifySignatureRequestParam.getSignatureAlgorithm());

            if (Strings.isNullOrEmpty(verifySignatureRequestParam.getSigningInput())) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(StringUtils.getErrorResponse(
                        "invalid_request",
                        "The request asked for an operation that cannot be supported because the signingInput parameter is mandatory."
                ));
            } else if (verifySignatureRequestParam.getSignature() == null) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(StringUtils.getErrorResponse(
                        "invalid_request",
                        "The request asked for an operation that cannot be supported because the signature parameter is mandatory."
                ));
            } else if (signatureAlgorithm == null) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(StringUtils.getErrorResponse(
                        "invalid_request",
                        "The request asked for an operation that cannot be supported because the server does not support the provided signatureAlgorithm parameter."
                ));
            } else if (signatureAlgorithm != SignatureAlgorithm.NONE
                    && SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily())
                    && Strings.isNullOrEmpty(verifySignatureRequestParam.getSharedSecret())) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(StringUtils.getErrorResponse(
                        "invalid_request",
                        "The request asked for an operation that cannot be supported because the shared secret parameter is mandatory."
                ));
            } else if (signatureAlgorithm != SignatureAlgorithm.NONE
                    && !SignatureAlgorithmFamily.HMAC.equals(signatureAlgorithm.getFamily()) // EC or RSA
                    && Strings.isNullOrEmpty(verifySignatureRequestParam.getAlias())) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(StringUtils.getErrorResponse(
                        "invalid_request",
                        "The request asked for an operation that cannot be supported because the alias parameter is mandatory."
                ));
            } else {
                boolean verified = pkcs11Service.verifySignature(
                        verifySignatureRequestParam.getSigningInput(), verifySignatureRequestParam.getSignature(),
                        verifySignatureRequestParam.getAlias(),
                        verifySignatureRequestParam.getSharedSecret(),
                        verifySignatureRequestParam.getJwksRequestParam(),
                        signatureAlgorithm);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put(VERIFIED, verified);

                builder.entity(jsonObject.toString());
            }
        } catch (NoSuchAlgorithmException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (KeyStoreException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (JSONException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        } catch (Exception e) {
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
