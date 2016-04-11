/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.license.LicenseService;
import org.xdi.oxd.server.op.IOperation;
import org.xdi.oxd.server.op.OperationFactory;

import java.io.IOException;

/**
 * oxD operation processor.
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/07/2013
 */
public class Processor {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    private final LicenseService licenseService;

    public Processor(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * Processed command.
     *
     * @param p_command command as string
     * @return response as string
     */
    public String process(String p_command) {
        LOG.trace("Command: {}", p_command);
        try {
            if (StringUtils.isNotBlank(p_command)) {
                final Command command = CoreUtils.createJsonMapper().readValue(p_command, Command.class);
                enforceLicenseRestrictions(command);
                final CommandResponse response = process(command);
                if (response != null) {
                    final String json = CoreUtils.asJson(response);
                    LOG.trace("Send back response: {}", json);
                    return json;
                } else {
                    LOG.error("There is no response produced by Processor.");
                    return null;
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.trace("No command or it's corrupted. Stop handling commands for this client.");
        return CommandResponse.INTERNAL_ERROR_RESPONSE_AS_STRING;
    }

    private void enforceLicenseRestrictions(Command command) {
        try {
            // if thread count 1 and no valid license then force 0.5 seconds delay
            if (licenseService.getThreadsCount() == 1 && !licenseService.isLicenseValid()) {
                forceWait(500);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void forceWait(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public CommandResponse process(Command p_command) {
        if (p_command != null) {
            try {
                final IOperation iOperation = OperationFactory.create(p_command, ServerLauncher.getInjector());
                if (iOperation != null) {
                    return iOperation.execute();
                } else {
                    return CommandResponse.OPERATION_IS_NOT_SUPPORTED;
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
