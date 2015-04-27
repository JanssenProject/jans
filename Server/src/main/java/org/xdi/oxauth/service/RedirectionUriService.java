/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.model.registration.Client;

import javax.ws.rs.HttpMethod;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version 0.9 April 27, 2015
 */
@Name("redirectionUriService")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class RedirectionUriService {

    @Logger
    private Log log;

    @In
    private ClientService clientService;

    public String validateRedirectionUri(String clientIdentifier, String redirectionUri) {
        try {
            Client client = clientService.getClient(clientIdentifier);

            if (client != null) {
                String sectorIdentifierUri = client.getSectorIdentifierUri();
                String[] redirectUris = client.getRedirectUris();

                if (StringUtils.isNotBlank(sectorIdentifierUri)) {
                    ClientRequest clientRequest = new ClientRequest(sectorIdentifierUri);
                    clientRequest.setHttpMethod(HttpMethod.GET);

                    ClientResponse<String> clientResponse = clientRequest.get(String.class);
                    int status = clientResponse.getStatus();

                    if (status == 200) {
                        String entity = clientResponse.getEntity(String.class);
                        JSONArray sectorIdentifierJsonArray = new JSONArray(entity);
                        redirectUris = new String[sectorIdentifierJsonArray.length()];
                        for (int i = 0; i < sectorIdentifierJsonArray.length(); i++) {
                            redirectUris[i] = sectorIdentifierJsonArray.getString(i);
                        }
                    } else {
                        return null;
                    }
                }

                if (StringUtils.isNotBlank(redirectionUri)) {
                    log.debug("Validating redirection URI: clientIdentifier = {0}, redirectionUri = {1}, found = {2}",
                            clientIdentifier, redirectionUri, redirectUris.length);

                    for (String uri : redirectUris) {
                        log.debug("Comparing {0} == {1}", uri, redirectionUri);
                        if ((uri.equals(redirectionUri) && getParams(uri).size() == 0) ||
                                uri.equals(redirectionUri) && getParams(uri).size() > 0 && compareParams(redirectionUri, uri)) {
                            return redirectionUri;
                        }
                    }
                } else {
                    // Accept Request Without redirect_uri when One Registered
                    if (redirectUris != null && redirectUris.length == 1) {
                        return redirectUris[0];
                    }
                }
            }
        } catch (URISyntaxException e) {
            return null;
        } catch (UnknownHostException e) {
            return null;
        } catch (ConnectException e) {
            return null;
        } catch (JSONException e) {
            return null;
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public String validatePostLogoutRedirectUri(String clientId, String postLogoutRedirectUri) {
        Client client = clientService.getClient(clientId);

        if (client != null) {
            String[] postLogoutRedirectUris = client.getPostLogoutRedirectUris();

            if (postLogoutRedirectUris != null & StringUtils.isNotBlank(postLogoutRedirectUri)) {
                log.debug("Validating post logout redirect URI: clientId = {0}, postLogoutRedirectUri = {1}",
                        clientId, postLogoutRedirectUri);

                for (String uri : postLogoutRedirectUris) {
                    log.debug("Comparing {0} == {1}", uri, postLogoutRedirectUri);
                    if (uri.equals(postLogoutRedirectUri)) {
                        return postLogoutRedirectUri;
                    }
                }
            } else {
                // Accept Request Without post_logout_redirect_uri when One Registered
                if (postLogoutRedirectUris != null && postLogoutRedirectUris.length == 1) {
                    return postLogoutRedirectUris[0];
                }
            }
        }

        return null;
    }

    private Map<String, String> getParams(String uri) {
        Map<String, String> params = new HashMap<String, String>();

        if (uri != null) {
            int paramsIndex = uri.indexOf("?");
            if (paramsIndex != -1) {
                String queryString = uri.substring(paramsIndex + 1);
                params = QueryStringDecoder.decode(queryString);
            }
        }
        return params;
    }

    private boolean compareParams(String uri1, String uri2) {
        if (StringUtils.isBlank(uri1) || StringUtils.isBlank(uri2)) {
            return false;
        }

        Map<String, String> params1 = getParams(uri1);
        Map<String, String> params2 = getParams(uri2);

        return params1.equals(params2);
    }
}