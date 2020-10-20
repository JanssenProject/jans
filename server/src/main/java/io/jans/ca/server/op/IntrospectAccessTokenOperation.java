package io.jans.ca.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.ca.common.Command;
import io.jans.ca.common.params.IntrospectAccessTokenParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;
import io.jans.ca.server.service.IntrospectionService;

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
    public IOpResponse execute(IntrospectAccessTokenParams params) {
        getValidationService().validate(params);

        final IntrospectionService introspectionService = getInstance(IntrospectionService.class);
        IntrospectionResponse response = introspectionService.introspectToken(params.getRpId(), params.getAccessToken());

        return new POJOResponse(response);
    }
}
