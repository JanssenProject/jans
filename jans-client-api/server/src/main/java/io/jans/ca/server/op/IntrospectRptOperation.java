package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.jans.ca.common.Command;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.params.IntrospectRptParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.POJOResponse;

/**
 * @author yuriyz
 */
public class IntrospectRptOperation extends BaseOperation<IntrospectRptParams> {

    /**
     * Base constructor
     *
     * @param command  command
     * @param injector injector
     */
    protected IntrospectRptOperation(Command command, Injector injector) {
        super(command, injector, IntrospectRptParams.class);
    }

    @Override
    public IOpResponse execute(IntrospectRptParams params) {
        getValidationService().validate(params);

        CorrectRptIntrospectionResponse response = getIntrospectionService().introspectRpt(params.getRpId(), params.getRpt());
        return new POJOResponse(response);
    }
}
