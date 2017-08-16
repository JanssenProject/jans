/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.session;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public class EndSessionParamsValidator {

    public static boolean isValidParams(String idTokenHint, String sessionId) {
        return StringHelper.isNotEmpty(idTokenHint) || StringHelper.isNotEmpty(sessionId);
    }

    public static void validateParams(String idTokenHint, String sessionId, String postLogoutUrl, ErrorResponseFactory errorFactory) {
        if (!isValidParams(idTokenHint, sessionId))
            errorFactory.throwBadRequestException(EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION);
        else if (StringUtils.isEmpty(postLogoutUrl))
            errorFactory.throwBadRequestException(EndSessionErrorResponseType.POST_LOGOUT_URI_NOT_PASSED);
    }

    public static void validateParams(String idTokenHint, String sessionId, ErrorResponseFactory errorFactory) {
        if (!isValidParams(idTokenHint, sessionId))
            errorFactory.throwBadRequestException(EndSessionErrorResponseType.INVALID_GRANT_AND_SESSION);
    }
}