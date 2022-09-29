/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.session.EndSessionErrorResponseType;
import io.jans.as.model.util.QueryStringDecoder;
import io.jans.as.model.util.Util;
import io.jans.as.common.model.session.SessionId;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
@Stateless
@Named
public class RedirectionUriService {

    private static final Logger log = LoggerFactory.getLogger(RedirectionUriService.class);

    @Inject
    private ClientService clientService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private LocalResponseCache localResponseCache;

    public String validateRedirectionUri(String clientIdentifier, String redirectionUri) {
        Client client = clientService.getClient(clientIdentifier);
        if (client == null) {
            return null;
        }
        return validateRedirectionUri(client, redirectionUri);
    }

    public List<String> getSectorRedirectUris(String sectorIdentiferUri) {
        List<String> result = Lists.newArrayList();
        if (StringUtils.isBlank(sectorIdentiferUri)) {
            return result;
        }

        final List<String> sectorRedirectUris = localResponseCache.getSectorRedirectUris(sectorIdentiferUri);
        if (sectorRedirectUris != null) {
            return sectorRedirectUris;
        }

        jakarta.ws.rs.client.Client clientRequest = ClientBuilder.newClient();

        String entity = null;
        try {
            Response clientResponse = clientRequest.target(sectorIdentiferUri).request().buildGet().invoke();
            int status = clientResponse.getStatus();
            if (status != 200) {
                return result;
            }

            entity = clientResponse.readEntity(String.class);
        } finally {
            clientRequest.close();
        }

        JSONArray sectorIdentifierJsonArray = new JSONArray(entity);

        for (int i = 0; i < sectorIdentifierJsonArray.length(); i++) {
            result.add(sectorIdentifierJsonArray.getString(i));
        }
        localResponseCache.putSectorRedirectUris(sectorIdentiferUri, result);
        return result;
    }

    public String validateRedirectionUri(@NotNull Client client, String redirectionUri) {
        try {
            String sectorIdentifierUri = client.getSectorIdentifierUri();
            String[] redirectUris = client.getRedirectUris();

            if (StringUtils.isNotBlank(sectorIdentifierUri)) {
                redirectUris = getSectorRedirectUris(sectorIdentifierUri).toArray(new String[0]);
            }

            if (StringUtils.isBlank(redirectionUri) && redirectUris != null && redirectUris.length == 1) {
                log.trace("First redirect_uri is returned.");
                return redirectUris[0];
            }

            if (StringUtils.isNotBlank(redirectionUri)) {
                if (redirectUris != null) {
                    log.trace("Validating redirection URI: clientIdentifier = {}, redirectionUri = {}, found = {}",
                            client.getClientId(), redirectionUri, redirectUris.length);
                    if (isUriEqual(redirectionUri, redirectUris)) {
                        log.trace("Redirect URI 'equals' found, clientId = {}, redirectionUri = {}", client.getClientId(), redirectionUri);

                        return redirectionUri;
                    } else {
                        log.trace("RedirectionUri didn't match with any of the client redirect uris, clientId = {}, redirectionUri = {}", client.getClientId(), redirectionUri);
                    }
                }

                if (BooleanUtils.isTrue(appConfiguration.getRedirectUrisRegexEnabled())) {
                    if (redirectionUri.matches(client.getAttributes().getRedirectUrisRegex())) {
                        log.trace("RedirectionUri is allowed by regexp, clientId = {}, redirectionUri = {}, regexp = {}", client.getClientId(), redirectionUri, client.getAttributes().getRedirectUrisRegex());
                        return redirectionUri;
                    } else {
                        log.trace("RedirectionUri didn't match with client regular expression, clientId = {}, redirectionUri = {}", client.getClientId(), redirectionUri);
                    }
                }
            } else {
                log.warn("RedirectionUri is blank, clientId = {}", client.getClientId());
            }
        } catch (Exception e) {
            log.error(String.format("Problems validating redirection uri, clientId = %s, redirectionUri = %s", client.getClientId(), redirectionUri), e);
            return null;
        }
        return null;
    }

    public static boolean isUriEqual(String redirectionUri, String[] redirectUris) {
        final String redirectUriWithoutParams = uriWithoutParams(redirectionUri);

        for (String uri : redirectUris) {
            log.debug("Comparing {} == {}", uri, redirectionUri);
            if (uri.equals(redirectionUri)) { // compare complete uri
                return true;
            }

            String uriWithoutParams = uriWithoutParams(uri);
            final Map<String, String> params = getParams(uri);

            if ((uriWithoutParams.equals(redirectUriWithoutParams) && params.size() == 0 && getParams(redirectionUri).size() == 0) ||
                    uriWithoutParams.equals(redirectUriWithoutParams) && params.size() > 0 && compareParams(redirectionUri, uri)) {
                return true;
            }
        }
        return false;
    }


    public String validatePostLogoutRedirectUri(String clientId, String postLogoutRedirectUri) {

        boolean isBlank = Util.isNullOrEmpty(postLogoutRedirectUri);

        Client client = clientService.getClient(clientId);

        if (client != null) {
            String[] postLogoutRedirectUris = client.getPostLogoutRedirectUris();
            log.debug("Validating post logout redirect URI: clientId = {}, postLogoutRedirectUri = {}", clientId, postLogoutRedirectUri);

            return validatePostLogoutRedirectUri(postLogoutRedirectUri, postLogoutRedirectUris);
        }

        if (!isBlank) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT, "`post_logout_redirect_uri` is not added to associated client.");
        }

        return null;
    }

    public String validatePostLogoutRedirectUri(SessionId sessionId, String postLogoutRedirectUri) {
        if (sessionId == null) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, EndSessionErrorResponseType.SESSION_NOT_PASSED, "Session object is not found.");
        }
        if (Strings.isNullOrEmpty(postLogoutRedirectUri)) {
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_PASSED, "`post_logout_redirect_uri` is empty.");
        }

        final Set<Client> clientsByDns = sessionId.getPermissionGrantedMap() != null
                ? clientService.getClient(sessionId.getPermissionGrantedMap().getClientIds(true), true)
                : Sets.newHashSet();

        log.trace("Validating post logout redirect URI: postLogoutRedirectUri = {}", postLogoutRedirectUri);

        for (Client client : clientsByDns) {
            String[] postLogoutRedirectUris = client.getPostLogoutRedirectUris();

            String validatedUri = validatePostLogoutRedirectUri(postLogoutRedirectUri, postLogoutRedirectUris);

            if (StringUtils.isNotBlank(validatedUri)) {
                return validatedUri;
            }
        }

        throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_ASSOCIATED_WITH_CLIENT, "Unable to validate `post_logout_redirect_uri`");
    }

    public String validatePostLogoutRedirectUri(String postLogoutRedirectUri, String[] allowedPostLogoutRedirectUris) {
        if (BooleanUtils.isTrue(appConfiguration.getAllowPostLogoutRedirectWithoutValidation())) {
            return postLogoutRedirectUri;
        }

        if (allowedPostLogoutRedirectUris != null && StringUtils.isNotBlank(postLogoutRedirectUri)) {
            if (isUriEqual(postLogoutRedirectUri, allowedPostLogoutRedirectUris)) {
                return postLogoutRedirectUri;
            }
        } else {
            // Accept Request Without post_logout_redirect_uri when One Registered
            if (allowedPostLogoutRedirectUris != null && allowedPostLogoutRedirectUris.length == 1) {
                return allowedPostLogoutRedirectUris[0];
            }
        }
        return "";
    }

    public static Map<String, String> getParams(String uri) {
        Map<String, String> params = new HashMap<>();

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