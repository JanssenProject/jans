/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server;

import com.google.inject.Inject;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.params.IParams;
import org.xdi.oxd.server.op.IOperation;
import org.xdi.oxd.server.op.OperationFactory;
import org.xdi.oxd.server.service.ValidationService;

import javax.ws.rs.WebApplicationException;

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
                    throw new HttpException(ErrorResponseCode.UNSUPPORTED_OPERATION);
                }
            } catch (ClientResponseFailure e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw new WebApplicationException((String) e.getResponse().getEntity(String.class), e.getResponse().getStatus());
            } catch (WebApplicationException e) {
                LOG.error(e.getLocalizedMessage(), e);
                throw e;
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
        }
        throw HttpException.internalError();
    }

}
