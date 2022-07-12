package io.jans.ca.server.op;

import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.RemoveSiteParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RemoveSiteResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.RpService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Named
public class RemoveSiteOperation extends BaseOperation<RemoveSiteParams> {

    @Inject
    RpService rpService;

    @Override
    public IOpResponse execute(RemoveSiteParams params, HttpServletRequest httpRequest) {
        String rpId = getRp(params).getRpId();
        if (rpService.remove(rpId)) {
            return new RemoveSiteResponse(rpId);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_REMOVE_SITE);
    }

    @Override
    public Class<RemoveSiteParams> getParameterClass() {
        return RemoveSiteParams.class;
    }

    @Override
    public boolean isAuthorizationRequired() {
        return false;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

}
