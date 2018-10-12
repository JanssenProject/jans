/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.*;
import org.xdi.oxd.common.params.IParams;
import org.xdi.oxd.server.op.IOperation;
import org.xdi.oxd.server.op.OperationFactory;
import org.xdi.oxd.server.service.ValidationService;

import java.io.IOException;

/**
 * oxD operation processor.
 *
 * @author Yuriy Zabrovarnyy
 */
public class Processor {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);

    private final ValidationService validationService;

    @Inject
    public Processor(ValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Processed command.
     *
     * @param p_command command as string
     * @return response as string
     */
    public String process(String p_command) {
        LOG.trace("Command: {}", StringUtils.remove(p_command, "client_secret"));
        try {
            if (StringUtils.isNotBlank(p_command)) {
                final Command command = CoreUtils.createJsonMapper().readValue(p_command, Command.class);
                final CommandResponse response = process(command);
                if (response != null) {
                    final String json = CoreUtils.asJson(response);
                    LOG.trace("Send back response: {}", CoreUtils.cleanUpLog(json));
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

    public CommandResponse process(Command command) {
        if (command != null) {
            try {
                final IOperation<IParams> operation = (IOperation<IParams>) OperationFactory.create(command, ServerLauncher.getInjector());
                if (operation != null) {
                    IParams iParams = Convertor.asParams(operation.getParameterClass(), command);
                    validationService.validate(iParams);

                    CommandResponse operationResponse = operation.execute(iParams);
                    if (operationResponse != null) {
                        return operationResponse;
                    } else {
                        LOG.error("No response from operation. Command: " + command);
                    }
                } else {
                    LOG.error("Operation is not supported!");
                    return CommandResponse.OPERATION_IS_NOT_SUPPORTED;
                }
            } catch (ErrorResponseException e) {
                LOG.error(e.getLocalizedMessage(), e);
                return CommandResponse.createErrorResponse(e.getErrorResponseCode());
            } catch (HttpErrorResponseException e) {
                LOG.error(e.getLocalizedMessage(), e);
                return CommandResponse.createErrorResponse(e.createErrorResponse());
            } catch (ClientResponseFailure e) {
                LOG.error(e.getLocalizedMessage(), e);
                return CommandResponse.createErrorResponse(new HttpErrorResponseException(e).createErrorResponse());
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

}
