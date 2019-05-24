/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.util;

import java.net.MalformedURLException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.gluu.oxauth.model.common.ResponseMode;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version October 1, 2015
 */
public class RedirectUtil {

	private final static Logger log = LoggerFactory.getLogger(RedirectUtil.class);

    static String JSON_REDIRECT_PROPNAME = "redirect";

    static String NO_REDIRECT_HEADER = "X-Gluu-NoRedirect";

    static int HTTP_REDIRECT = 302;

    public static ResponseBuilder getRedirectResponseBuilder(RedirectUri redirectUriResponse, HttpServletRequest httpRequest) {
        ResponseBuilder builder;

        if (httpRequest != null && httpRequest.getHeader(NO_REDIRECT_HEADER) != null) {
            try {
                URI redirectURI = URI.create(redirectUriResponse.toString());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(JSON_REDIRECT_PROPNAME, redirectURI.toURL());
                String jsonResp = jsonObject.toString();
                jsonResp = jsonResp.replace("\\/", "/");
                builder = Response.ok(
                        new GenericEntity<String>(jsonResp, String.class),
                        MediaType.APPLICATION_JSON_TYPE
                );

            } catch (MalformedURLException e) {
                builder = Response.serverError();
                log.debug(e.getMessage(), e);
            } catch (JSONException e) {
                builder = Response.serverError();
                log.debug(e.getMessage(), e);
            }
        } else if (redirectUriResponse.getResponseMode() != ResponseMode.FORM_POST) {
            URI redirectURI = URI.create(redirectUriResponse.toString());
            builder = new ResponseBuilderImpl();
            builder = Response.status(HTTP_REDIRECT);
            builder.location(redirectURI);
        } else {
            builder = new ResponseBuilderImpl();
            builder.status(Response.Status.OK);
            builder.type(MediaType.TEXT_HTML_TYPE);
            builder.cacheControl(CacheControl.valueOf("no-cache, no-store"));
            builder.header("Pragma", "no-cache");
            builder.entity(redirectUriResponse.toString());
        }

        return builder;
    }
}
