/*
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package io.jans.ca.server;

import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.IParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.op.*;
import io.jans.ca.server.service.ServiceProvider;
import io.jans.ca.server.utils.Convertor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;

/**
 * client-api operation processor.
 *
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class Processor {
    @Inject
    Logger logger;
    @Inject
    ServiceProvider serviceProvider;

    public IOpResponse process(Command command) {
        if (command != null) {
            try {
                final IOperation<IParams> operation = (IOperation<IParams>) create(command);
                if (operation != null) {
                    IParams iParams = Convertor.asParams(operation.getParameterClass(), command);
                    serviceProvider.getValidationService().validate(iParams);

                    IOpResponse operationResponse = operation.execute(iParams);
                    if (operationResponse != null) {
                        return operationResponse;
                    } else {
                        logger.error("No response from operation. Command: {}", command);
                    }
                } else {
                    logger.error("Operation is not supported! null");
                    throw new HttpException(ErrorResponseCode.UNSUPPORTED_OPERATION);
                }
            } catch (ClientErrorException e) {
                throw new WebApplicationException(e.getResponse().readEntity(String.class), e.getResponse().getStatus());
            } catch (WebApplicationException e) {
                logger.error(e.getLocalizedMessage(), e);
                throw e;
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }
        throw HttpException.internalError();
    }

    private IOperation<? extends IParams> create(Command command) {

        if (command != null && command.getCommandType() != null) {
            switch (command.getCommandType()) {
//                case REGISTER_SITE:
//                    return new RegisterSiteOperation(command, serviceProvider);
//                case UPDATE_SITE:
//                    return new UpdateSiteOperation(command, serviceProvider);
                case REMOVE_SITE:
                    return new RemoveSiteOperation(command, serviceProvider);
                case GET_CLIENT_TOKEN:
                    return new GetClientTokenOperation(command, serviceProvider);
                case GET_ACCESS_TOKEN_BY_REFRESH_TOKEN:
                    return new GetAccessTokenByRefreshTokenOperation(command, serviceProvider);
                case INTROSPECT_ACCESS_TOKEN:
                    return new IntrospectAccessTokenOperation(command, serviceProvider);
                case GET_USER_INFO:
                    return new GetUserInfoOperation(command, serviceProvider);
                case GET_JWKS:
                    return new GetJwksOperation(command, serviceProvider);
//                case GET_DISCOVERY:
//                    return new GetDiscoveryOperation(command, serviceProvider);
                case GET_AUTHORIZATION_URL:
                    return new GetAuthorizationUrlOperation(command, serviceProvider);
                case GET_TOKENS_BY_CODE:
                    return new GetTokensByCodeOperation(command, serviceProvider);
                case GET_LOGOUT_URI:
                    return new GetLogoutUrlOperation(command, serviceProvider);
                case RS_PROTECT:
                    return new RsProtectOperation(command, serviceProvider);
                case RS_CHECK_ACCESS:
                    return new RsCheckAccessOperation(command, serviceProvider);
                case INTROSPECT_RPT:
                    return new IntrospectRptOperation(command, serviceProvider);
                case RP_GET_RPT:
                    return new RpGetRptOperation(command, serviceProvider);
                case RP_GET_CLAIMS_GATHERING_URL:
                    return new RpGetGetClaimsGatheringUrlOperation(command, serviceProvider);
                case GET_RP:
                    return new GetRpOperation(command, serviceProvider);
                case GET_RP_JWKS:
                    return new GetRpJwksOperation(command, serviceProvider);
                case GET_AUTHORIZATION_CODE:
                    return new GetAuthorizationCodeOperation(command, serviceProvider);
                case AUTHORIZATION_CODE_FLOW:
                    return new AuthorizationCodeFlowOperation(command, serviceProvider);
                case GET_REQUEST_OBJECT_JWT:
                    return new GetRequestObjectOperation(command, serviceProvider);
                case RS_MODIFY:
                    return new RsModifyOperation(command, serviceProvider);
                case VALIDATE:
                    return new ValidateOperation(command, serviceProvider);
                case IMPLICIT_FLOW:
                    return new ImplicitFlowOperation(command, serviceProvider);
                case CHECK_ACCESS_TOKEN:
                    return new CheckAccessTokenOperation(command, serviceProvider);
                case CHECK_ID_TOKEN:
                    return new CheckIdTokenOperation(command, serviceProvider);
                case ISSUER_DISCOVERY:
                    return new GetIssuerOperation(command, serviceProvider);
                case GET_REQUEST_URI:
                    return new GetRequestObjectUriOperation(command, serviceProvider);
            }
            logger.error("Command is not supported. Command: {}", command);
        } else {
            logger.error("Command is invalid. Command: {}", command);
        }
        return null;
    }

}
