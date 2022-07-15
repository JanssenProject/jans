package io.jans.ca.server.op;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.ca.common.params.IntrospectAccessTokenParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.service.IntrospectionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Named
public class IntrospectAccessTokenOperation extends BaseOperation<IntrospectAccessTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectAccessTokenOperation.class);
    @Inject
    IntrospectionService introspectionService;

    @Override
    public IOpResponse execute(IntrospectAccessTokenParams params, HttpServletRequest httpServletRequest) {
        getValidationService().validate(params);

        IntrospectionResponse response = introspectionService.introspectToken(params.getRpId(), params.getAccessToken());

        return new POJOResponse(response);
    }

    @Override
    public Class<IntrospectAccessTokenParams> getParameterClass() {
        return IntrospectAccessTokenParams.class;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

}
