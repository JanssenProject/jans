/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.interception.CIBASupportInterception;
import org.gluu.oxauth.interception.CIBASupportInterceptionInterface;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Interceptor
@CIBASupportInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBASupportInterceptor implements CIBASupportInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBASupportInterceptor.class);

    @AroundInvoke
    public Object isCIBASupported(InvocationContext ctx) {
        log.trace("CIBA: support...");

        boolean result = false;
        try {
            result = isCIBASupported();
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to process CIBA support.", e);
        }

        return result;
    }

    @Override
    public boolean isCIBASupported() {
        return true;
    }
}