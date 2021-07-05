package io.jans.as.client.par;

import io.jans.as.client.BaseResponseWithErrors;
import io.jans.as.model.authorize.AuthorizeErrorResponseType;
import io.jans.as.model.error.IErrorType;
import org.jboss.resteasy.client.ClientResponse;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Yuriy Zabrovarnyy
 */
public class ParResponse extends BaseResponseWithErrors {

    private String requestUri;
    private Integer expiresIn;

    public ParResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);
        parseJson(entity);
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public IErrorType fromString(String str) {
        return AuthorizeErrorResponseType.fromString(str);
    }

    private void parseJson(String entity) {
        try {
            JSONObject jsonObj = new JSONObject(entity);
            requestUri = jsonObj.optString("request_uri");
            expiresIn = jsonObj.optInt("expires_in");
        } catch (JSONException e) {
            // ignore
        }
    }
}