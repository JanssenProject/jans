/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.model.Jwks;
import org.jboss.resteasy.client.ClientResponse;

import java.io.IOException;

/**
 * @author Javier Rojas Blum
 * @version April 12, 2016
 */
public class JwksResponse extends BaseResponse {

    private Jwks jwks;

    public JwksResponse(ClientResponse<String> clientResponse) throws IOException {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            ObjectMapper mapper = new ObjectMapper();
            jwks = mapper.readValue(jsonObject.toString(), Jwks.class);
        }
    }

    public Jwks getJwks() {
        return jwks;
    }
}
