package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.params.RemoveSiteParams;
import org.xdi.oxd.common.response.RemoveSiteResponse;

/**
 * @author yuriyz
 */
public class RemoveSiteOperation extends BaseOperation<RemoveSiteParams> {

//    private static final Logger LOG = LoggerFactory.getLogger(RemoveSiteOperation.class);

    /**
     * Base constructor
     *
     * @param command  command
     * @param injector injector
     */
    protected RemoveSiteOperation(Command command, Injector injector) {
        super(command, injector, RemoveSiteParams.class);
    }

    @Override
    public CommandResponse execute(RemoveSiteParams params) throws Exception {
        String oxdId = getRp().getOxdId();
        if (getRpService().remove(oxdId)) {
            return okResponse(new RemoveSiteResponse(oxdId));
        }
        return CommandResponse.createErrorResponse(ErrorResponseCode.FAILED_TO_REMOVE_SITE);
    }
}
