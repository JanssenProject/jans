/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import java.net.MalformedURLException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;

public class RedirectUtil {

    private final static Log LOG = Logging.getLog(RedirectUtil.class);

    static String JSON_REDIRECT_PROPNAME = "redirect";

    static String NO_REDIRECT_HEADER = "X-Gluu-NoRedirect";

    static int HTTP_REDIRECT = 302;

    public static ResponseBuilder getRedirectResponseBuilder(String location, HttpServletRequest httpRequest) {

        URI redirectURI = URI.create(location);
        ResponseBuilder builder;

        if (httpRequest == null || httpRequest.getHeader(NO_REDIRECT_HEADER) == null) {
            builder = Response.status(HTTP_REDIRECT);
            builder.location(redirectURI);
        } else {
            try {
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
        }

        return builder;
    }
}
