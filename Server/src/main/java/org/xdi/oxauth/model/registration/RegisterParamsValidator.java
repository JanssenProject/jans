package org.xdi.oxauth.model.registration;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.register.ApplicationType;
import org.xdi.oxauth.model.util.Util;

/**
 * Validates the parameters received for the register web service.
 *
 * @author Javier Rojas Date: 01.13.2012
 */
public class RegisterParamsValidator {

    private final static Log LOG = Logging.getLog(RegisterParamsValidator.class);

    /**
     * Validates the parameters for a register request.
     *
     * @param applicationType     native or web.
     * @param redirectUris        Space-separated list of redirect URIs.
     * @param sectorIdentifierUrl A HTTPS scheme URL to be used in calculating Pseudonymous Identifiers by the OP.
     *                            The URL contains a file with a single JSON array of redirect_uri values.
     * @return Whether the parameters of client register is valid or not.
     */
    public static boolean validateParamsClientRegister(ApplicationType applicationType, List<String> redirectUris,
                                                       String sectorIdentifierUrl) {
        boolean validParams = applicationType != null && redirectUris != null && !redirectUris.isEmpty();

        if (validParams && StringUtils.isNotBlank(sectorIdentifierUrl)) {
            try {
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

    public static boolean validateRedirectUris(ApplicationType applicationType, List<String> redirectUris) {
        final String HTTP = "http";
        final String HTTPS = "https";
        final String LOCALHOST = "localhost";

        try {
            if (redirectUris != null && !redirectUris.isEmpty()) {
                for (String redirectUri : redirectUris) {
                    if (redirectUri == null || redirectUri.contains("#")) {
                        return false;
                    } else {
                        URI uri = new URI(redirectUri);
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


        return true;
    }
}