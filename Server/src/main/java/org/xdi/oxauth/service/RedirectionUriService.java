/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.util.Util;

import javax.ws.rs.HttpMethod;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    @In
    private ErrorResponseFactory errorResponseFactory;

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

                    final String redirectUriWithoutParams = uriWithoutParams(redirectionUri);

                    for (String uri : redirectUris) {
                        log.debug("Comparing {0} == {1}", uri, redirectionUri);
                        if (uri.equals(redirectionUri)) { // compare complete uri
                            return redirectionUri;
                        }

                        String uriWithoutParams = uriWithoutParams(uri);
                        final Map<String, String> params = getParams(uri);

                        if ((uriWithoutParams.equals(redirectUriWithoutParams) && params.size() == 0 && getParams(redirectionUri).size() == 0) ||
                                uriWithoutParams.equals(redirectUriWithoutParams) && params.size() > 0 && compareParams(redirectionUri, uri)) {
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
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    public String validatePostLogoutRedirectUri(Optional<SessionState> sessionState, String postLogoutRedirectUri) {
        if (Strings.isNullOrEmpty(postLogoutRedirectUri) || !sessionState.isPresent()) {
            return "";
        }

        final Set<Client> clientsByDns = sessionState.get().getPermissionGrantedMap() != null ?
                clientService.getClient(sessionState.get().getPermissionGrantedMap().keySet(), true) :
                Sets.<Client>newHashSet();

        log.trace("Validating post logout redirect URI: postLogoutRedirectUri = {0}", postLogoutRedirectUri);

        for (Client client : clientsByDns) {
            String[] postLogoutRedirectUris = client.getPostLogoutRedirectUris();
            if (postLogoutRedirectUris == null) {
                continue;
            }

            for (String uri : postLogoutRedirectUris) {
                log.debug("Comparing {0} == {1}, clientId: {2}", uri, postLogoutRedirectUri, client.getClientId());
                if (uri.equals(postLogoutRedirectUri)) {
                    return postLogoutRedirectUri;
                }
            }
        }
        log.trace("Unable to find postLogoutRedirectUri = {0}", postLogoutRedirectUri);
        return "";
    }


    public String validatePostLogoutRedirectUri(String clientId, String postLogoutRedirectUri) {

        boolean isBlank = Util.isNullOrEmpty(postLogoutRedirectUri);

        Client client = clientService.getClient(clientId);

        if (client != null) {
            String[] postLogoutRedirectUris = client.getPostLogoutRedirectUris();

            if (postLogoutRedirectUris != null && StringUtils.isNotBlank(postLogoutRedirectUri)) {
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

        if (!isBlank) {
            errorResponseFactory.throwBadRequestException(EndSessionErrorResponseType.INVALID_REQUEST);
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

    private String uriWithoutParams(String uri) {
        if (uri != null) {
            int paramsIndex = uri.indexOf("?");
            if (paramsIndex != -1) {
                return uri.substring(0, paramsIndex);
            }
        }
        return uri;
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