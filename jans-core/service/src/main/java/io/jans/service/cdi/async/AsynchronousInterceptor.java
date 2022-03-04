/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cdi.async;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * @author Yuriy Movchan Date: 07/07/2017
 */
@Interceptor
@Asynchronous
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AsynchronousInterceptor implements Serializable {

    private static final long serialVersionUID = 4839412676894893540L;

    private static final ThreadLocal<Boolean> ASYNC_INVOCATION = new ThreadLocal<Boolean>();

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
        if (Boolean.TRUE.equals(ASYNC_INVOCATION.get())) {
            return ctx.proceed();
        }

        final InvocationContext localCtx = ctx;
        return CompletableFuture.supplyAsync(new Supplier<Object>() {
            @Override
            public Object get() {
                try {
                    ASYNC_INVOCATION.set(Boolean.TRUE);
                    return localCtx.proceed();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    ASYNC_INVOCATION.remove();
                }

                return null;
            }
        });
    }

}
