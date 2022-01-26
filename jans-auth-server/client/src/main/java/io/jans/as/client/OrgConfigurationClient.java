/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;


public class OrgConfigurationClient extends BaseClient<OrgConfigurationRequest, OrgConfigurationResponse> {

    private static final Logger LOG = Logger.getLogger(OrgConfigurationClient.class);

    private static final String MEDIA_TYPES = String.join(",", MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON);
    
    public static final String FAVICON_SERVLET_PATH = "/servlet/favicon";
    public static final String LOGO_SERVLET_PATH = "/servlet/logo";

    
    public OrgConfigurationClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.GET;
    }
    
    public String getFaviconUrl(String opHost, String faviconPath) {
        String result = baseOpUrl(opHost);
        if (StringUtils.isNotBlank(faviconPath)) {
            result += faviconPath;
        }
        return result + FAVICON_SERVLET_PATH;
    }
    
    private String baseOpUrl(String opHost) {
        if (!opHost.startsWith("http")) {
            opHost = "https://" + opHost;
        }
        if (opHost.endsWith("/")) {
            opHost = StringUtils.removeEnd(opHost, "/");
        }
        return opHost;
    }

    /**
     * Executes the call to the REST Service requesting the OpenID Configuration and processes the response.
     *
     * @return The service response.
     */
    private OrgConfigurationResponse exec() {
        LOG.error("\n\n\n OrgConfigurationResponse:::exec() - Entry \n\n\n" );
        try {
            LOG.error("\n\n\n OrgConfigurationResponse:::exec() - Exit \n\n\n" );
        } catch (JSONException e) {
            LOG.error("There is an error in the JSON response. Check if there is a syntax error in the JSON response or there is a wrong key", e);
            
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            LOG.error(e.getMessage(), e); // Unexpected exception.
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    public static void parse(String json, OrgConfigurationResponse response) {
        LOG.error("\n\n\n OrgConfigurationResponse:::parse() - Entry - json = "+json+", response = "+response);
       
        
        LOG.error("\n\n\n OrgConfigurationResponse:::parse() - Exit - json = "+json+", response = "+response);
    }

    public static OrgConfigurationResponse parse(String json) {
        LOG.error("\n\n\n OrgConfigurationResponse:::parse() - Entry-2 - json = "+json);
        OrgConfigurationResponse response = new OrgConfigurationResponse();
        parse(json, response);
        LOG.error("\n\n\n OrgConfigurationResponse:::parse() - Exit - response = "+response);
        return response;
    }
}
