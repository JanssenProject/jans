package org.gluu.oxeleven.client;

import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public class JwksResponse extends BaseResponse {

    public JwksResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);
    }
}
