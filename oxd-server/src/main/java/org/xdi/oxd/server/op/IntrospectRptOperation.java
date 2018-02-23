package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.xdi.oxd.common.params.IntrospectRptParams;

/**
 * @author yuriyz
 */
public class IntrospectRptOperation extends BaseOperation<IntrospectRptParams> {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectRptOperation.class);

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
    public CommandResponse execute(IntrospectRptParams params) throws Exception {
        getValidationService().validate(params);

        CorrectRptIntrospectionResponse response = getIntrospectionService().introspectRpt(params.getOxdId(), params.getRpt());
        return CommandResponse.ok().setData(new POJONode(response));
    }
}
