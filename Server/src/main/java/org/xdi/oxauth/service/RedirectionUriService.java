/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.slf4j.Logger;
import org.xdi.oxauth.client.QueryStringDecoder;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.session.EndSessionErrorResponseType;
import org.xdi.oxauth.model.util.Util;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
@Stateless
@Named
public class RedirectionUriService {

    @Inject
    private Logger log;

    @Inject
    private ClientService clientService;

    @Inject
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

                if (StringUtils.isNotBlank(redirectionUri) && redirectUris != null) {
                    log.debug("Validating redirection URI: clientIdentifier = {}, redirectionUri = {}, found = {}",
                            clientIdentifier, redirectionUri, redirectUris.length);

                    final String redirectUriWithoutParams = uriWithoutParams(redirectionUri);

                    for (String uri : redirectUris) {
                        log.debug("Comparing {} == {}", uri, redirectionUri);
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

    public String validatePostLogoutRedirectUri(String clientId, String postLogoutRedirectUri) {

        boolean isBlank = Util.isNullOrEmpty(postLogoutRedirectUri);

        Client client = clientService.getClient(clientId);

        if (client != null) {
            String[] postLogoutRedirectUris = client.getPostLogoutRedirectUris();

            if (postLogoutRedirectUris != null && StringUtils.isNotBlank(postLogoutRedirectUri)) {
                log.debug("Validating post logout redirect URI: clientId = {}, postLogoutRedirectUri = {}",
                        clientId, postLogoutRedirectUri);

                for (String uri : postLogoutRedirectUris) {
                    log.debug("Comparing {} == {}", uri, postLogoutRedirectUri);
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
            errorResponseFactory.throwBadRequestException(EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT);
        }

        return null;
    }

	public String validatePostLogoutRedirectUri(SessionId sessionId, String postLogoutRedirectUri) {
        if (sessionId == null) {
            errorResponseFactory.throwBadRequestException(EndSessionErrorResponseType.SESSION_NOT_PASSED);
            return null;
        }
        if (Strings.isNullOrEmpty(postLogoutRedirectUri)) {
            errorResponseFactory.throwBadRequestException(EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_PASSED);
            return null;
        }

        final Set<Client> clientsByDns = sessionId.getPermissionGrantedMap() != null
                ? clientService.getClient(sessionId.getPermissionGrantedMap().getClientIds(true), true)
                : Sets.<Client>newHashSet();

        log.trace("Validating post logout redirect URI: postLogoutRedirectUri = {}", postLogoutRedirectUri);

        for (Client client : clientsByDns) {
            String[] postLogoutRedirectUris = client.getPostLogoutRedirectUris();
            if (postLogoutRedirectUris == null) {
                continue;
            }

            for (String uri : postLogoutRedirectUris) {
                log.debug("Comparing {} == {}, clientId: {}", uri, postLogoutRedirectUri, client.getClientId());
                if (uri.equals(postLogoutRedirectUri)) {
                    return postLogoutRedirectUri;
                }
            }
        }

        errorResponseFactory.throwBadRequestException(EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT);
        return null;
    }

    public static Map<String, String> getParams(String uri) {
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

    public static String uriWithoutParams(String uri) {
        if (uri != null) {
            int paramsIndex = uri.indexOf("?");
            if (paramsIndex != -1) {
                return uri.substring(0, paramsIndex);
            }
        }
        return uri;
    }

    public static boolean compareParams(String uri1, String uri2) {
        if (StringUtils.isBlank(uri1) || StringUtils.isBlank(uri2)) {
            return false;
        }

        Map<String, String> params1 = getParams(uri1);
        Map<String, String> params2 = getParams(uri2);

        return params1.equals(params2);
    }
}