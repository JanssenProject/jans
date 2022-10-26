/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.rest;

import static io.jans.eleven.model.DeleteKeyResponseParam.DELETED;

import java.security.KeyStoreException;
import java.security.PublicKey;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;

import io.jans.eleven.service.PKCS11Service;
import io.jans.eleven.util.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.google.common.base.Strings;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
@Path("/")
public class DeleteKeyRestServiceImpl implements DeleteKeyRestService {

	@Inject
	private Logger log;
	
	@Inject
	private PKCS11Service pkcs11Service;

    public Response deleteKey(String alias) {
        Response.ResponseBuilder builder = Response.ok();

        try {
            if (Strings.isNullOrEmpty(alias)) {
                builder = Response.status(Response.Status.BAD_REQUEST);
                builder.entity(StringUtils.getErrorResponse(
                        "invalid_request",
                        "The request asked for an operation that cannot be supported because the alias parameter is mandatory."
                ));
            } else {
                PublicKey publicKey = pkcs11Service.getPublicKey(alias);
                if (publicKey == null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(DELETED, false);

                    builder.entity(jsonObject.toString());
                } else {
                	pkcs11Service.deleteKey(alias);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(DELETED, true);

                    builder.entity(jsonObject.toString());
                }
            }
        } catch (KeyStoreException e) {
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
