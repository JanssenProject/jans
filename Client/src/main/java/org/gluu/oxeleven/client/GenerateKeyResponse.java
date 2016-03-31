package org.gluu.oxeleven.client;

import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientResponse;

import static org.gluu.oxeleven.model.GenerateKeyResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public class GenerateKeyResponse extends BaseResponse {

    private String alias;
    private String algorithm;
    private String curve;

    public GenerateKeyResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        JSONObject jsonObject = getJSONEntity();
        if (jsonObject != null) {
            alias = jsonObject.optString(ALIAS);
            algorithm = jsonObject.optString(ALGORITHM);
            curve = jsonObject.optString(CURVE);
        }
    }

    public String getAlias() {
        return alias;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getCurve() {
        return curve;
    }
}
