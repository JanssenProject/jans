/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.IParams;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class OperationFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OperationFactory.class);

    private OperationFactory() {
    }

    public static IOperation<? extends IParams> create(Command command, final Injector injector) {
        if (command != null && command.getCommandType() != null) {
            switch (command.getCommandType()) {
                case AUTHORIZATION_CODE_FLOW:
                    return new AuthorizationCodeFlowOperation(command, injector);
                case CHECK_ID_TOKEN:
                    return new CheckIdTokenOperation(command, injector);
                case CHECK_ACCESS_TOKEN:
                    return new CheckAccessTokenOperation(command, injector);
                case GET_AUTHORIZATION_URL:
                    return new GetAuthorizationUrlOperation(command, injector);
                case GET_TOKENS_BY_CODE:
                    return new GetTokensByCodeOperation(command, injector);
                case GET_USER_INFO:
                    return new GetUserInfoOperation(command, injector);
                case VALIDATE:
                    return new ValidateOperation(command, injector);
                case IMPLICIT_FLOW:
                    return new ImplicitFlowOperation(command, injector);
                case GET_ACCESS_TOKEN_BY_REFRESH_TOKEN:
                    return new GetAccessTokenByRefreshTokenOperation(command, injector);
                case REGISTER_SITE:
                    return new RegisterSiteOperation(command, injector);
                case GET_AUTHORIZATION_CODE:
                    return new GetAuthorizationCodeOperation(command, injector);
                case GET_LOGOUT_URI:
                    return new GetLogoutUrlOperation(command, injector);
                case UPDATE_SITE:
                    return new UpdateSiteOperation(command, injector);
                case RS_PROTECT:
                    return new RsProtectOperation(command, injector);
                case RS_CHECK_ACCESS:
                    return new RsCheckAccessOperation(command, injector);
                case RP_GET_RPT:
                    return new RpGetRptOperation(command, injector);
                case RP_GET_CLAIMS_GATHERING_URL:
                    return new RpGetGetClaimsGatheringUrlOperation(command, injector);
                case GET_CLIENT_TOKEN:
                    return new GetClientTokenOperation(command, injector);
                case INTROSPECT_ACCESS_TOKEN:
                    return new IntrospectAccessTokenOperation(command, injector);
                case INTROSPECT_RPT:
                    return new IntrospectRptOperation(command, injector);
                case REMOVE_SITE:
                    return new RemoveSiteOperation(command, injector);
                case GET_RP:
                    return new GetRpOperation(command, injector);
                case GET_JWKS:
                    return new GetJwksOperation(command, injector);
                case GET_OP_DISCOVERY_CONFIG:
                    return new GetOpDiscoveryConfigOperation(command, injector);
            }
            LOG.error("Command is not supported. Command: {}", command);
        } else {
            LOG.error("Command is invalid. Command: {}", command);
        }
        return null;
    }
}
