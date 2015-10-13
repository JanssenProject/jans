/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.common.ResponseMode;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.net.MalformedURLException;
import java.net.URI;

/**
 * @version October 1, 2015
 */
public class RedirectUtil {

    private final static Log LOG = Logging.getLog(RedirectUtil.class);

    static String JSON_REDIRECT_PROPNAME = "redirect";

    static String NO_REDIRECT_HEADER = "X-Gluu-NoRedirect";

    static int HTTP_REDIRECT = 302;

    public static ResponseBuilder getRedirectResponseBuilder(RedirectUri redirectUriResponse, HttpServletRequest httpRequest) {
        ResponseBuilder builder = null;

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
                LOG.debug(e.getMessage(), e);
            } catch (JSONException e) {
                builder = Response.serverError();
                LOG.debug(e.getMessage(), e);
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
