/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.registration;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.register.RegisterErrorResponseType;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the parameters received for the register web service.
 *
 * @author Javier Rojas Blum
 * @version September 1, 2015
 */
public class RegisterParamsValidator {

    private static final Log LOG = Logging.getLog(RegisterParamsValidator.class);

    private static final String HTTP = "http";
    private static final String HTTPS = "https";
    private static final String LOCALHOST = "localhost";

    /**
     * Validates the parameters for a register request.
     *
     * @param applicationType     The Application Type: native or web.
     * @param subjectType         The subject_type requested for responses to this Client.
     * @param redirectUris        Space-separated list of redirect URIs.
     * @param sectorIdentifierUrl A HTTPS scheme URL to be used in calculating Pseudonymous Identifiers by the OP.
     *                            The URL contains a file with a single JSON array of redirect_uri values.
     * @return Whether the parameters of client register is valid or not.
     */
    public static boolean validateParamsClientRegister(ApplicationType applicationType, SubjectType subjectType,
                                                       List<String> redirectUris, String sectorIdentifierUrl) {
        boolean validParams = applicationType != null && redirectUris != null && !redirectUris.isEmpty();

        if (subjectType == null || !ConfigurationFactory.instance().getConfiguration().getSubjectTypesSupported().contains(subjectType.toString())) {
            LOG.debug("Parameter subject_type is not valid.");
            return false;
        }

        if (validParams && StringUtils.isNotBlank(sectorIdentifierUrl)) {
            try {
                URI uri = new URI(sectorIdentifierUrl);
                if (!HTTPS.equalsIgnoreCase(uri.getScheme())) {
                    return false;
                }

                ClientRequest clientRequest = new ClientRequest(sectorIdentifierUrl);
                clientRequest.setHttpMethod(HttpMethod.GET);

                ClientResponse<String> clientResponse = clientRequest.get(String.class);
                int status = clientResponse.getStatus();

                if (status == 200) {
                    String entity = clientResponse.getEntity(String.class);

                    JSONArray sectorIdentifierJsonArray = new JSONArray(entity);
                    return Util.asList(sectorIdentifierJsonArray).containsAll(redirectUris);
                }
            } catch (URISyntaxException e) {
                LOG.trace(e.getMessage(), e);
                return false;
            } catch (UnknownHostException e) {
                LOG.trace(e.getMessage(), e);
                return false;
            } catch (ConnectException e) {
                LOG.trace(e.getMessage(), e);
                return false;
            } catch (JSONException e) {
                LOG.trace(e.getMessage(), e);
                return false;
            } catch (Exception e) {
                LOG.trace(e.getMessage(), e);
                return false;
            }
        }

        return validParams;
    }

    /**
     * Validates the parameters for a client read request.
     *
     * @param clientId    Unique Client identifier.
     * @param accessToken Access Token obtained out of band to authorize the registrant.
     * @return Whether the parameters of client read is valid or not.
     */
    public static boolean validateParamsClientRead(String clientId, String accessToken) {
        return StringUtils.isNotBlank(clientId) && StringUtils.isNotBlank(accessToken);
    }

    /**
     * @param applicationType     The Application Type: native or web.
     * @param subjectType         Subject Type requested for responses to this Client.
     * @param redirectUris        Redirection URI values used by the Client.
     * @param sectorIdentifierUrl A HTTPS scheme URL to be used in calculating Pseudonymous Identifiers by the OP.
     *                            The URL contains a file with a single JSON array of redirect_uri values.
     * @return Whether the Redirect URI parameters are valid or not.
     */
    public static boolean validateRedirectUris(ApplicationType applicationType, SubjectType subjectType,
                                               List<String> redirectUris, String sectorIdentifierUrl) {
        Set<String> redirectUriHosts = new HashSet<String>();

        try {
            if (redirectUris != null && !redirectUris.isEmpty()) {
                for (String redirectUri : redirectUris) {
                    if (redirectUri == null || redirectUri.contains("#")) {
                        return false;
                    } else {
                        URI uri = new URI(redirectUri);
                        redirectUriHosts.add(uri.getHost());
                        switch (applicationType) {
                            case WEB:
                                if (!HTTPS.equalsIgnoreCase(uri.getScheme())) {
                                    return false;
                                } else if (LOCALHOST.equalsIgnoreCase(uri.getHost())) {
                                    return false;
                                }
                                break;
                            case NATIVE:
                                if (!HTTP.equalsIgnoreCase(uri.getScheme())) {
                                    return false;
                                } else if (!LOCALHOST.equalsIgnoreCase(uri.getHost())) {
                                    return false;
                                }
                                break;
                        }
                    }
                }
            } else {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }

        /*
         * Providers that use pairwise sub (subject) values SHOULD utilize the sector_identifier_uri value
         * provided in the Subject Identifier calculation for pairwise identifiers.
         *
         * If the Client has not provided a value for sector_identifier_uri in Dynamic Client Registration,
         * the Sector Identifier used for pairwise identifier calculation is the host component of the
         * registered redirect_uri.
         *
         * If there are multiple hostnames in the registered redirect_uris, the Client MUST register a
         * sector_identifier_uri.
         */
        if (subjectType != null && subjectType.equals(SubjectType.PAIRWISE) && StringUtils.isBlank(sectorIdentifierUrl)) {
            if (redirectUriHosts.size() > 1) {
                return false;
            }
        }

        return true;
    }

    public static void validateLogoutUri(String logoutUri, List<String> redirectUris, ErrorResponseFactory errorResponseFactory) {
        if (Strings.isNullOrEmpty(logoutUri)) { // logout uri is optional so null or empty string is valid
            return;
        }

        // preconditions
        if (redirectUris == null || redirectUris.isEmpty()) {
            LOG.error("Preconditions of logout uri validation are failed.");
            throwInvalidLogoutUri(errorResponseFactory);
            return;
        }

        try {
            Set<String> redirectUriHosts = collectUriHosts(redirectUris);

            URI uri = new URI(logoutUri);

            if (!redirectUriHosts.contains(uri.getHost())) {
                LOG.error("logout uri host is not within redirect_uris, logout_uri: {0}, redirect_uris: {1}", logoutUri, redirectUris);
                throwInvalidLogoutUri(errorResponseFactory);
                return;
            }

            if (!HTTPS.equalsIgnoreCase(uri.getScheme())) {
                LOG.error("logout uri schema is not https, logout_uri: {0}", logoutUri);
                throwInvalidLogoutUri(errorResponseFactory);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throwInvalidLogoutUri(errorResponseFactory);
        }
    }

    private static void throwInvalidLogoutUri(ErrorResponseFactory errorResponseFactory) throws WebApplicationException {
        throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST.getStatusCode()).
                        entity(errorResponseFactory.getErrorAsJson(RegisterErrorResponseType.INVALID_LOGOUT_URI)).
                        cacheControl(ServerUtil.cacheControl(true, false)).
                        header("Pragma", "no-cache").
                        build());
    }

    private static Set<String> collectUriHosts(List<String> uriList) throws URISyntaxException {
        Set<String> hosts = new HashSet<String>();

        for (String redirectUri : uriList) {
            URI uri = new URI(redirectUri);
            hosts.add(uri.getHost());
        }
        return hosts;
    }
}