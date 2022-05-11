package io.jans.ca.server.op;

import io.jans.ca.common.Command;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.IntrospectRptParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.service.IntrospectionService;
import io.jans.ca.server.service.ServiceProvider;

/**
 * @author yuriyz
 */
public class IntrospectRptOperation extends BaseOperation<IntrospectRptParams> {

    private IntrospectionService introspectionService;

    public IntrospectRptOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, IntrospectRptParams.class);
        this.introspectionService = serviceProvider.getIntrospectionService();
    }

    @Override
    public IOpResponse execute(IntrospectRptParams params) {
        getValidationService().validate(params);

        CorrectRptIntrospectionResponse response = introspectionService.introspectRpt(params.getRpId(), params.getRpt());
        return new POJOResponse(response);
    }
}
