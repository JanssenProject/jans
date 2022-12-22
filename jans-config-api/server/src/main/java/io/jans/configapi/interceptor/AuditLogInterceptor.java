/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import io.jans.configapi.core.interceptor.RequestAuditInterceptor;
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.util.AuthUtil;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@RequestAuditInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class AuditLogInterceptor {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogInterceptor.class);

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Inject
    AuthUtil authUtil;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {
        LOG.info("Audit Log Interceptor - context:{}", context);
        try {
            processRequest(context);

        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
        return context.proceed();
    }

    private void processRequest(InvocationContext context) {
        LOG.info("Audit Log Interceptor process - context:{}", context);
        Object[] ctxParameters = context.getParameters();
        Method method = context.getMethod();
        Class[] clazzArray = method.getParameterTypes();

        LOG.debug("Audit Log Interceptor process - ctxParameters:{}, method:{}, clazzArray:{}", ctxParameters, method,
                clazzArray);
        AUDIT_LOG.info("\n Audit Log Interceptor process - ctxParameters:{}, method:{}, clazzArray:{}", ctxParameters,
                method, clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {

                Object obj = ctxParameters[i];
                LOG.info("Request obj:{}", obj);
                // Audit log
                logAuditData(context, obj);

            }
        }
    }

    private <T> void logAuditData(InvocationContext context, T obj) {
        try {

            // Get Audit config
            AuditLogConf auditLogConf = getAuditLogConf();
            LOG.info("auditLogConf:{}", auditLogConf);
            if (auditLogConf != null && auditLogConf.isEnabled()) {
                AUDIT_LOG.info("====== Request for endpoint:{}, method:{}, from:{}, user:{}, data:{} ", info.getPath(),
                        context.getMethod(), request.getRemoteAddr(), httpHeaders.getHeaderString("User-inum"), obj);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private Map<String, String> getAuditHeaderAttributes(AuditLogConf auditLogConf) {
        LOG.info("AuditLogInterceptor::getAuditHeaderAttributes() - auditLogConf:{}", auditLogConf);
        if (auditLogConf == null) {
            return Collections.emptyMap();
        }
        List<String> attributes = auditLogConf.getHeaderAttributes();
        LOG.info("AuditLogInterceptor::getAuditHeaderAttributes() - attributes:{}", attributes);
        Map<String, String> attributeMap = null;
        if (attributes != null && !attributes.isEmpty()) {
            attributeMap = new HashMap<>();
            for (String attributeName : attributes) {

                String attributeValue = httpHeaders.getHeaderString(attributeName);
                attributeMap.put(attributeName, attributeValue);
            }
        }
        AUDIT_LOG.info("attributeMap:{} ", attributeMap);
        LOG.info("AuditLogInterceptor::getAuditHeaderAttributes() - attributeMap:{}", attributeMap);
        return attributeMap;
    }

}
