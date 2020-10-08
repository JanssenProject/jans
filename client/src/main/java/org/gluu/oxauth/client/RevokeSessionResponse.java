package org.gluu.oxauth.client;

import org.gluu.oxauth.model.session.EndSessionErrorResponseType;
import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RevokeSessionResponse extends BaseResponseWithErrors<EndSessionErrorResponseType>{

    public RevokeSessionResponse() {
    }

    public RevokeSessionResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);
        injectDataFromJson();
    }

    @Override
    public EndSessionErrorResponseType fromString(String params) {
        return EndSessionErrorResponseType.fromString(params);
    }

    public void injectDataFromJson() {
        injectDataFromJson(entity);
    }
}
