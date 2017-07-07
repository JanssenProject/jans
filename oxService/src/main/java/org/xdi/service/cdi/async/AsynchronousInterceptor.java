package org.xdi.service.cdi.async;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * @author Yuriy Movchan Date: 07/07/2017
 */
@Interceptor
@Asynchronous
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class AsynchronousInterceptor implements Serializable {

	private static final long serialVersionUID = 4839412676894893540L;

	private static final ThreadLocal<Boolean> asyncInvocation = new ThreadLocal<Boolean>();

    @AroundInvoke
    public Object invoke(InvocationContext ctx) throws Exception {
		if (Boolean.TRUE.equals(asyncInvocation.get())) {
			return ctx.proceed();
		}

		final InvocationContext localCtx = ctx;
		return CompletableFuture.supplyAsync(new Supplier<Object>() {
			@Override
			public Object get() {
				try {
					asyncInvocation.set(Boolean.TRUE);
					return localCtx.proceed();
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					asyncInvocation.remove();
				}

				return null;
			}
		});
	}

}
