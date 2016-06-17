package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.RpGetGatParams;
import org.xdi.oxd.common.response.RpGetRptResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

public class RpGetGatOperation extends BaseOperation<RpGetGatParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RpGetGatOperation.class);

    protected RpGetGatOperation(Command command, final Injector injector) {
        super(command, injector, RpGetGatParams.class);
    }

    @Override
    public CommandResponse execute(RpGetGatParams params) {
        validate(params);

        final RpGetRptResponse r = new RpGetRptResponse();
        r.setRpt(getUmaTokenService().getGat(params.getOxdId(), params.getScopes()));
        return okResponse(r);
    }

    private void validate(RpGetGatParams params) {
        if (params.getScopes() == null || params.getScopes().isEmpty()) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_REQUEST_SCOPES_REQUIRED);
        }
    }
}