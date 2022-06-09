package io.jans.ca.server.op;

import io.jans.as.model.common.IntrospectionResponse;
import io.jans.ca.common.Command;
import io.jans.ca.common.params.IntrospectAccessTokenParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.service.IntrospectionService;
import io.jans.ca.server.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuriyz
 */
public class IntrospectAccessTokenOperation extends BaseOperation<IntrospectAccessTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectAccessTokenOperation.class);

    private IntrospectionService introspectionService;

    public IntrospectAccessTokenOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider,IntrospectAccessTokenParams.class);
        this.introspectionService = serviceProvider.getIntrospectionService();
    }

    @Override
    public IOpResponse execute(IntrospectAccessTokenParams params) {
        getValidationService().validate(params);

        IntrospectionResponse response = introspectionService.introspectToken(params.getRpId(), params.getAccessToken());

        return new POJOResponse(response);
    }
}
