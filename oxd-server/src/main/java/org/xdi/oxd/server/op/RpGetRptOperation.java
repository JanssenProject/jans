/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.common.response.RpGetRptResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpGetRptOperation extends BaseOperation<RpGetRptParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RpGetRptOperation.class);

    protected RpGetRptOperation(Command command, final Injector injector) {
        super(command, injector, RpGetRptParams.class);
    }

    @Override
    public CommandResponse execute(RpGetRptParams params) {
        final RpGetRptResponse r = new RpGetRptResponse();
        r.setRpt(getUmaTokenService().getRpt(params.getOxdId(), params.isForceNew()));
        return okResponse(r);
    }
}
