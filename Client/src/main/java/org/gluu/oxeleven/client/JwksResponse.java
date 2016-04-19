/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.client;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;
import org.gluu.oxeleven.model.JwksRequestParam;
import org.jboss.resteasy.client.ClientResponse;

import java.io.IOException;

/**
 * @author Javier Rojas Blum
 * @version April 18, 2016
 */
public class JwksResponse extends BaseResponse {

    private JwksRequestParam jwksRequestParam;

    public JwksResponse(ClientResponse<String> clientResponse) throws IOException {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            ObjectMapper mapper = new ObjectMapper();
            jwksRequestParam = mapper.readValue(jsonObject.toString(), JwksRequestParam.class);
        }
    }

    public JwksRequestParam getJwksRequestParam() {
        return jwksRequestParam;
    }
}
