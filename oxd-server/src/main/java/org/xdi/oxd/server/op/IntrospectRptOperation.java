package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.xdi.oxd.common.params.IntrospectRptParams;
import org.xdi.oxd.common.response.IOpResponse;
import org.xdi.oxd.common.response.POJOResponse;

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
    public IOpResponse execute(IntrospectRptParams params) throws Exception {
        getValidationService().validate(params);

        CorrectRptIntrospectionResponse response = getIntrospectionService().introspectRpt(params.getOxdId(), params.getRpt());
        return new POJOResponse(response);
    }
}
