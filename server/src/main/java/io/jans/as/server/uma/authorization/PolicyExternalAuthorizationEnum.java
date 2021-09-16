/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.authorization;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */

public enum PolicyExternalAuthorizationEnum implements IPolicyExternalAuthorization {
    TRUE(true), FALSE(false);

    private final boolean m_result;

    PolicyExternalAuthorizationEnum(boolean p_result) {
        m_result = p_result;
    }

    @Override
    public boolean authorize(UmaAuthorizationContext p_authorizationContext) {
        return m_result;
    }
}
