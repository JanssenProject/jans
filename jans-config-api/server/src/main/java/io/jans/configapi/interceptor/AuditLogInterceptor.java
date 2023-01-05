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

        try {
            LOG.info("Audit Log Interceptor - context:{}, info:{}, request:{}, httpHeaders:{}, AUDIT_LOG:{}", context,
                    info, request, httpHeaders, AUDIT_LOG);
            AUDIT_LOG.info("\n ********************** Request ********************** ");

            // Get Audit config
            AuditLogConf auditLogConf = getAuditLogConf();
            LOG.info("auditLogConf:{}", auditLogConf);

            // Log if enabled
            if (auditLogConf != null && auditLogConf.isEnabled()) {

                // Request audit
                String beanClassName = context.getClass().getName();
                String method = context.getMethod().getName();

                AUDIT_LOG.info("endpoint:{}, beanClassName:{}, method:{}, from:{}, user:{} ", info.getPath(),
                        beanClassName, method, request.getRemoteAddr(), httpHeaders.getHeaderString("User-inum"));

                // Header attribute audit
                Map<String, String> headerData = getAuditHeaderAttributes(auditLogConf);
                AUDIT_LOG.info("headerData:{} ", headerData);

                // Request object audit
                processRequest(context, auditLogConf);
            }

        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
        return context.proceed();
    }

    private void processRequest(InvocationContext context, AuditLogConf auditLogConf) {
        LOG.info("Process Audit Log Interceptor - context:{}, auditLogConf:{}", context, auditLogConf);

        Object[] ctxParameters = context.getParameters();
        Method method = context.getMethod();
        Class[] clazzArray = method.getParameterTypes();

        LOG.debug("Audit Log Interceptor process - ctxParameters:{}, method:{}, clazzArray:{}", ctxParameters, method,
                clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Object obj = ctxParameters[i];
                LOG.info("Request obj:{}", obj);
                AUDIT_LOG.info("objectType:{}, obj:{} ", clazzArray[i], obj);

            }
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

        LOG.info("AuditLogInterceptor::getAuditHeaderAttributes() - attributeMap:{}", attributeMap);
        return attributeMap;
    }

}
