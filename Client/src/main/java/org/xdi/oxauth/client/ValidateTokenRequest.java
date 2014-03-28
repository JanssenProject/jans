package org.xdi.oxauth.client;

/**
 * Represents a validate token request to send to the authorization server.
 *
 * @author Javier Rojas Blum Date: 10.27.2011
 */
public class ValidateTokenRequest extends BaseRequest {

    private String accessToken;

    /**
     * Returns the access token.
     *
     * @return The access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token.
     *
     * @param accessToken The access token.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Returns a query string with the parameters of the validation request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        if (accessToken != null) {
            queryStringBuilder.append("access_token=").append(accessToken);
        }

        return queryStringBuilder.toString();
    }
}
