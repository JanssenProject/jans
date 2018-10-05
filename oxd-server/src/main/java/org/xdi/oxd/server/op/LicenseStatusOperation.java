/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.EmptyParams;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/11/2014
 */

public class LicenseStatusOperation extends BaseOperation<EmptyParams> {

    protected LicenseStatusOperation(Command command, final Injector injector) {
        super(command, injector, EmptyParams.class);
    }

    @Override
    public CommandResponse execute(EmptyParams params) {
        return CommandResponse.OPERATION_IS_NOT_SUPPORTED;
    }
}
