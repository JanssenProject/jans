package org.xdi.oxauth.client;

/**
 * Represents an OpenId Configuration request to send to the authorization server.
 *
 * @author Javier Rojas Blum Date: 12.6.2011
 */
public class OpenIdConfigurationRequest extends BaseRequest{

    /**
     * Construct an OpenID Configuration Request.
     */
    public OpenIdConfigurationRequest() {
    }

    @Override
    public String getQueryString() {
        return null;
    }
}