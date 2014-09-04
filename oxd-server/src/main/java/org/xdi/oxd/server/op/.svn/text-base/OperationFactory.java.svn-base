package org.xdi.oxd.server.op;

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

    public static IOperation create(Command p_command) {
        if (p_command != null && p_command.getCommandType() != null) {
            switch (p_command.getCommandType()) {
                case AUTHORIZE_RPT:
                    return new AuthorizeRptOperation(p_command);
                case OBTAIN_PAT:
                    return new ObtainPatOperation(p_command);
                case OBTAIN_AAT:
                    return new ObtainAatOperation(p_command);
                case OBTAIN_RPT:
                    return new ObtainRptOperation(p_command);
                case REGISTER_CLIENT:
                    return new RegisterClientOperation(p_command);
                case CLIENT_READ:
                    return new ClientReadOperation(p_command);
                case REGISTER_RESOURCE:
                    return new RegisterResourceOperation(p_command);
                case REGISTER_TICKET:
                    return new RegisterTicketOperation(p_command);
                case RPT_STATUS:
                    return new RptStatusOperation(p_command);
                case DISCOVERY:
                    return new DiscoveryOperation(p_command);
                case CHECK_ID_TOKEN:
                    return new CheckIdTokenOperation(p_command);
                case CHECK_ACCESS_TOKEN:
                    return new CheckAccessTokenOperation(p_command);
            }
            LOG.error("Command is not supported. Command: {}", p_command);
        } else {
            LOG.error("Command is invalid. Command: {}", p_command);
        }
        return null;
    }
}
