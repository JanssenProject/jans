package io.jans.scim.service.scim2.interceptor;

import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_ATTRIBUTES;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_EXCLUDED_ATTRS;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_FILTER;

import java.lang.annotation.Annotation;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.QueryParam;

import io.jans.scim.model.scim2.SearchRequest;
import org.slf4j.Logger;

/**
 * This class checks if filter, attributes or excludedAttributes query param contains resource references ($ref) and if so
 * drops the dollar sign of occurrences. This is required for introspection utilities to make their work more accurately.
 */
/*
 * This could have been implemented with a decorator, but pollutes the code a lot, so this way is more concise. Using a
 * resteasy filter is not convenient since it does not get invoked if the call is internal (not an HTTP one)
 */
@RefAdjusted
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class ReferenceURIInterceptor {

    @Inject
    private Logger log;

    @AroundInvoke
    public Object manage(InvocationContext ctx) throws Exception {

        Object[] params=ctx.getParameters();
        Annotation[][] annotations=ctx.getMethod().getParameterAnnotations();

        for (int i = 0; i<annotations.length; i++){
            //Iterate over annotations found at every parameter
            for (Annotation annotation : annotations[i]) {
                if (annotation instanceof QueryParam) {
                    String paramName=((QueryParam)annotation).value();

                    if ((paramName.equals(QUERY_PARAM_FILTER) || paramName.equals(QUERY_PARAM_ATTRIBUTES) ||
                            paramName.equals(QUERY_PARAM_EXCLUDED_ATTRS))){
                        log.trace("Removing '$' char (if any) from {} param", paramName);
                        params[i]=dropDollar(params[i]);
                    }
                }
            }
            if (params[i]!=null && params[i] instanceof SearchRequest){
                log.trace("Removing '$' char (if any) from SearchRequest object");
                SearchRequest sr=(SearchRequest) params[i];
                sr.setAttributes(dropDollar(sr.getAttributesStr()));
                sr.setExcludedAttributes(dropDollar(sr.getExcludedAttributesStr()));
                sr.setFilter(dropDollar(sr.getFilter()));
            }
        }
        log.debug("ReferenceURIInterceptor. manage exit");

        return ctx.proceed();

    }

    private static String dropDollar(Object param){
        return (param!=null && param instanceof String) ? param.toString().replaceAll("\\$ref", "ref") : null;
    }

}
