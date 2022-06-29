package io.jans.ca.server.op;

import io.jans.ca.common.CommandType;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.IntrospectRptParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.service.IntrospectionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@RequestScoped
@Named
public class IntrospectRptOperation extends TemplateOperation<IntrospectRptParams> {

    @Inject
    IntrospectionService introspectionService;

    @Override
    public IOpResponse execute(IntrospectRptParams params, HttpServletRequest httpServletRequest) {
        getValidationService().validate(params);

        CorrectRptIntrospectionResponse response = introspectionService.introspectRpt(params.getRpId(), params.getRpt());
        return new POJOResponse(response);
    }

    @Override
    public Class<IntrospectRptParams> getParameterClass() {
        return IntrospectRptParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.INTROSPECT_RPT;
    }
}
