/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import io.jans.configapi.core.interceptor.RequestAuditInterceptor;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.util.AuthUtil;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Interceptor
@RequestAuditInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditLogInterceptor {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogInterceptor.class);

    @Inject
    AuthUtil authUtil;

    @Inject
    ApiAppConfiguration apiAppConfiguration;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {

        try {

            // return if audit disabled
            if (apiAppConfiguration.isDisableAuditLogger()) {
                AUDIT_LOG.debug("Audit is disabled by disableAuditLogger config.");
                return context.proceed();
            }

            // Log for audit
            HttpServletRequest request = ((BaseResource) context.getTarget()).getHttpRequest();
            HttpHeaders httpHeaders = ((BaseResource) context.getTarget()).getHttpHeaders();
            UriInfo uriInfo = ((BaseResource) context.getTarget()).getUriInfo();

            // Get Audit config
            AuditLogConf auditLogConf = getAuditLogConf();
            String method = request.getMethod();

            // Log if enabled
            if (auditLogConf.isEnabled() && !ignoreMethod(method, auditLogConf)) {

                // Request audit
                String client = httpHeaders.getHeaderString("jans-client");
                String userInum = httpHeaders.getHeaderString("User-inum");

                // Log request without data
                AUDIT_LOG.info("User:{} {} {} using client:{}", userInum, getAction(method),
                        getResource(uriInfo.getPath()), client);

                if (auditLogConf.isLogData()) {
                    // Log request data
                    processRequest(context, auditLogConf);
                }
            }

        } catch (Exception ex) {
            LOG.error("Not able to log audit details due to error:{}", ex);
        }
        return context.proceed();
    }

    private void processRequest(InvocationContext context, AuditLogConf auditLogConf) {
        LOG.info("Process Audit Log Interceptor - context:{}, auditLogConf:{}", context, auditLogConf);

        Object[] ctxParameters = context.getParameters();
        Method method = context.getMethod();
        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        LOG.trace("Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ", paramCount, parameters,
                clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                LOG.trace("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                if (obj != null && (!obj.toString().toUpperCase().contains("PASSWORD")
                        || !obj.toString().toUpperCase().contains("SECRET"))) {
                    AUDIT_LOG.info("obj:{} ", obj);
                }
            }
        }
    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private boolean ignoreMethod(String method, AuditLogConf auditLogConf) {
        LOG.debug("Checking if method to be ignored - method:{}, auditLogConf:{}", method, auditLogConf);

        if (StringUtils.isBlank(method) || auditLogConf == null || auditLogConf.getIgnoreHttpMethod() == null
                || auditLogConf.getIgnoreHttpMethod().isEmpty()) {
            return false;
        }

        if (auditLogConf.getIgnoreHttpMethod().contains(method)) {
            return true;
        }

        return false;
    }

    private boolean ignoreMethod(InvocationContext context, AuditLogConf auditLogConf) {
        LOG.debug("Checking if method to be ignored - context:{}, auditLogConf:{}", context, auditLogConf);

        if (auditLogConf == null || context.getMethod().getAnnotations() == null
                || context.getMethod().getAnnotations().length <= 0) {
            return false;
        }

        for (int i = 0; i < context.getMethod().getAnnotations().length; i++) {
            LOG.debug("Check if method is to be ignored - context.getMethod().getAnnotations()[i]:{} ",
                    context.getMethod().getAnnotations()[i]);

            if (context.getMethod().getAnnotations()[i] != null && auditLogConf.getIgnoreHttpMethod() != null
                    && auditLogConf.getIgnoreHttpMethod()
                            .contains(context.getMethod().getAnnotations()[i].toString())) {
                return true;
            }

        }
        return false;
    }

    private String getAction(String method) {
        String action = null;
        if (StringUtils.isNotBlank(method)) {
            switch (method) {
            case "POST":
                action = "added";
                break;
            case "PUT":
            case "PATCH":
                action = "changed";
                break;
            case "DELETE":
                action = "deleted";
                break;
            default:
                action = "fetched";
                break;
            }
        }
        return action;
    }

    private String getResource(String path) {
        if (StringUtils.isNotBlank(path)) {
            path = path.replace("/", "-");
        }
        return path;
    }

}
