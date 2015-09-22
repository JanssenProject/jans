/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class OperationFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OperationFactory.class);

    private OperationFactory() {
    }

    public static IOperation create(Command p_command, final Injector injector) {
        if (p_command != null && p_command.getCommandType() != null) {
            switch (p_command.getCommandType()) {
                case AUTHORIZE_RPT:
                    return new AuthorizeRptOperation(p_command, injector);
                case AUTHORIZATION_CODE_FLOW:
                    return new AuthorizationCodeFlowOperation(p_command, injector);
                case OBTAIN_PAT:
                    return new ObtainPatOperation(p_command, injector);
                case OBTAIN_AAT:
                    return new ObtainAatOperation(p_command, injector);
                case OBTAIN_RPT:
                    return new ObtainRptOperation(p_command, injector);
                case REGISTER_CLIENT:
                    return new RegisterClientOperation(p_command, injector);
                case CLIENT_READ:
                    return new ClientReadOperation(p_command, injector);
                case REGISTER_RESOURCE:
                    return new RegisterResourceOperation(p_command, injector);
                case REGISTER_TICKET:
                    return new RegisterTicketOperation(p_command, injector);
                case RPT_STATUS:
                    return new RptStatusOperation(p_command, injector);
                case DISCOVERY:
                    return new DiscoveryOperation(p_command, injector);
                case CHECK_ID_TOKEN:
                    return new CheckIdTokenOperation(p_command, injector);
                case CHECK_ACCESS_TOKEN:
                    return new CheckAccessTokenOperation(p_command, injector);
                case LICENSE_STATUS:
                    return new LicenseStatusOperation(p_command, injector);
                case GET_AUTHORIZATION_URL:
                    return new GetAuthorizationUrlOperation(p_command, injector);
                case GET_TOKENS_BY_CODE:
                    return new GetTokensByCodeOperation(p_command, injector);
            }
            LOG.error("Command is not supported. Command: {}", p_command);
        } else {
            LOG.error("Command is invalid. Command: {}", p_command);
        }
        return null;
    }
}
