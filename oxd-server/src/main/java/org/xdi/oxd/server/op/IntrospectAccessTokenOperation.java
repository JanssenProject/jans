package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.params.IntrospectAccessTokenParams;
import org.xdi.oxd.common.response.IOpResponse;
import org.xdi.oxd.common.response.POJOResponse;
import org.xdi.oxd.server.service.IntrospectionService;

/**
 * @author yuriyz
 */
public class IntrospectAccessTokenOperation extends BaseOperation<IntrospectAccessTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectAccessTokenOperation.class);

    /**
     * Base constructor
     *
     * @param command  command
     * @param injector injector
     */
    protected IntrospectAccessTokenOperation(Command command, Injector injector) {
        super(command, injector, IntrospectAccessTokenParams.class);
    }

    @Override
    public IOpResponse execute(IntrospectAccessTokenParams params) throws Exception {
        getValidationService().validate(params);

        final IntrospectionService introspectionService = getInstance(IntrospectionService.class);
        IntrospectionResponse response = introspectionService.introspectToken(params.getOxdId(), params.getAccessToken());

        return new POJOResponse(response);
    }
}
