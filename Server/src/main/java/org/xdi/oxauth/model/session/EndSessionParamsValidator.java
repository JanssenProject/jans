/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.session;

import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 *
 * Date: 12.15.2011
 */
public class EndSessionParamsValidator {

    public static boolean isValidParams(String idTokenHint, String sessionState) {
        return StringHelper.isNotEmpty(idTokenHint) || StringHelper.isNotEmpty(sessionState);
    }

    public static void validateParams(String idTokenHint, String sessionState, String postLogoutUrl, ErrorResponseFactory errorFactory) {
        if (!isValidParams(idTokenHint, sessionState) || (postLogoutUrl == null) || postLogoutUrl.isEmpty()) {
            errorFactory.throwBadRequestException(EndSessionErrorResponseType.INVALID_REQUEST);
        }
    }

    public static void validateParams(String idTokenHint, String sessionState, ErrorResponseFactory errorFactory) {
        if (!isValidParams(idTokenHint, sessionState)) {
            errorFactory.throwBadRequestException(EndSessionErrorResponseType.INVALID_REQUEST);
        }
    }
}