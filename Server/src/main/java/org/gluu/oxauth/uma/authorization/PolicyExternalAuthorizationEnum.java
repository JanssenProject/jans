/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.uma.authorization;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */

public enum PolicyExternalAuthorizationEnum implements IPolicyExternalAuthorization {
    TRUE(true), FALSE(false);

    private final boolean m_result;

    private PolicyExternalAuthorizationEnum(boolean p_result) {
        m_result = p_result;
    }

    @Override
    public boolean authorize(UmaAuthorizationContext p_authorizationContext) {
        return m_result;
    }
}
