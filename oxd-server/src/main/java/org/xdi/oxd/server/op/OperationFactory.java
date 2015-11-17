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

    public static IOperation create(Command command, final Injector injector) {
        if (command != null && command.getCommandType() != null) {
            switch (command.getCommandType()) {
                case AUTHORIZE_RPT:
                    return new AuthorizeRptOperation(command, injector);
                case AUTHORIZATION_CODE_FLOW:
                    return new AuthorizationCodeFlowOperation(command, injector);
                case OBTAIN_PAT:
                    return new ObtainPatOperation(command, injector);
                case OBTAIN_AAT:
                    return new ObtainAatOperation(command, injector);
                case OBTAIN_RPT:
                    return new ObtainRptOperation(command, injector);
                case REGISTER_CLIENT:
                    return new RegisterClientOperation(command, injector);
                case CLIENT_READ:
                    return new ClientReadOperation(command, injector);
                case REGISTER_RESOURCE:
                    return new RegisterResourceOperation(command, injector);
                case REGISTER_TICKET:
                    return new RegisterTicketOperation(command, injector);
                case RPT_STATUS:
                    return new RptStatusOperation(command, injector);
                case DISCOVERY:
                    return new DiscoveryOperation(command, injector);
                case CHECK_ID_TOKEN:
                    return new CheckIdTokenOperation(command, injector);
                case CHECK_ACCESS_TOKEN:
                    return new CheckAccessTokenOperation(command, injector);
                case LICENSE_STATUS:
                    return new LicenseStatusOperation(command, injector);
                case GET_AUTHORIZATION_URL:
                    return new GetAuthorizationUrlOperation(command, injector);
                case GET_TOKENS_BY_CODE:
                    return new GetTokensByCodeOperation(command, injector);
                case GET_USER_INFO:
                    return new GetUserInfoOperation(command, injector);
                case IMPLICIT_FLOW:
                    return new ImplicitFlowOperation(command, injector);
                case REGISTER_SITE:
                    return new RegisterSiteOperation(command, injector);
                case GET_AUTHORIZATION_CODE:
                    return new GetAuthorizationCodeOperation(command, injector);
                case LOGOUT:
                    return new LogoutOperation(command, injector);
            }
            LOG.error("Command is not supported. Command: {}", command);
        } else {
            LOG.error("Command is invalid. Command: {}", command);
        }
        return null;
    }
}
