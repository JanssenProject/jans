/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.rest;

import static org.gluu.oxeleven.model.DeleteKeyResponseParam.DELETED;

import java.security.KeyStoreException;
import java.security.PublicKey;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxeleven.service.PKCS11Service;
import org.gluu.oxeleven.util.StringUtils;
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
