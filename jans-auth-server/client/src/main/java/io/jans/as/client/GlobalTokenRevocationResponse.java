package io.jans.as.client;

import io.jans.as.model.session.EndSessionErrorResponseType;
import jakarta.ws.rs.core.Response;

/**
 * @author Yuriy Z
 */
public class GlobalTokenRevocationResponse extends BaseResponseWithErrors<EndSessionErrorResponseType> {

    public GlobalTokenRevocationResponse() {
    }

    public GlobalTokenRevocationResponse(Response clientResponse) {
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
