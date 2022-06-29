package io.jans.ca.server.op;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.ca.common.Command;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.params.IntrospectAccessTokenParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.service.IntrospectionService;
import io.jans.ca.server.service.ServiceProvider;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequestScoped
@Named
public class IntrospectAccessTokenOperation extends TemplateOperation<IntrospectAccessTokenParams> {

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
    public CommandType getCommandType() {
        return CommandType.INTROSPECT_ACCESS_TOKEN;
    }
}
