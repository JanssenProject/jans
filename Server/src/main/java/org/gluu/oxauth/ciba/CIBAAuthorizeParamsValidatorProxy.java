/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.interception.CIBAAuthorizeParamsValidatorInterception;
import org.gluu.oxauth.interception.CIBAAuthorizeParamsValidatorInterceptionInterface;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.model.error.DefaultErrorResponse;

import javax.ejb.Stateless;
import javax.inject.Named;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Stateless
@Named
public class CIBAAuthorizeParamsValidatorProxy implements CIBAAuthorizeParamsValidatorInterceptionInterface {

    @Override
    @CIBAAuthorizeParamsValidatorInterception
    public DefaultErrorResponse validateParams(
            List<String> scopeList, String clientNotificationToken, BackchannelTokenDeliveryMode tokenDeliveryMode,
            String loginHintToken, String idTokenHint, String loginHint, String bindingMessage,
            Boolean backchannelUserCodeParameter, String userCode) {
        return null;
    }
}