/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import io.jans.configapi.core.interceptor.RequestAuditInterceptor;
import io.jans.configapi.core.rest.BaseResource;
import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.util.AuthUtil;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
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

    @Inject
    AuthUtil authUtil;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {

        try {
            LOG.debug("Audit Log Interceptor - context:{}, AUDIT_LOG:{}", context, AUDIT_LOG);

            HttpServletRequest request = ((BaseResource) context.getTarget()).getHttpRequest();
            HttpHeaders httpHeaders = ((BaseResource) context.getTarget()).getHttpHeaders();
            UriInfo uriInfo = ((BaseResource) context.getTarget()).getUriInfo();
            LOG.debug("Audit Log Interceptor -request:{}, httpHeaders:{}, uriInfo:{}", request, httpHeaders, uriInfo);

            // Get Audit config
            AuditLogConf auditLogConf = getAuditLogConf();
            LOG.debug("auditLogConf:{}, ignoreMethod(context):{}", auditLogConf, ignoreMethod(context, auditLogConf));

            // Log if enabled
            if (!ignoreMethod(context, auditLogConf)) {
                AUDIT_LOG.info("\n ********************** Audit Request Detail Start ********************** ");
                // Request audit
                String beanClassName = context.getClass().getName();
                String method = context.getMethod().getName();

                AUDIT_LOG.info("endpoint:{}, beanClassName:{}, method:{}, from:{}, user:{} ", uriInfo.getPath(),
                        beanClassName, method, request.getRemoteAddr(), httpHeaders.getHeaderString("User-inum"));

                // Header attribute audit
                Map<String, String> headerData = getAuditHeaderAttributes(auditLogConf, httpHeaders);
                AUDIT_LOG.info("headerData:{} ", headerData);

                // Request object audit
                processRequest(context, auditLogConf);
                AUDIT_LOG.info("********************** Audit Request Detail End ********************** ");
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

        AUDIT_LOG.info("RequestReaderInterceptor - Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ",
                paramCount, parameters, clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                AUDIT_LOG.info("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                AUDIT_LOG.info("RequestReaderInterceptor final - obj -  obj:{} ", obj);

            }
        }
    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
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

    private Map<String, String> getAuditHeaderAttributes(AuditLogConf auditLogConf, HttpHeaders httpHeaders) {
        LOG.info("AuditLogInterceptor::getAuditHeaderAttributes() - auditLogConf:{}, httpHeaders:{}", auditLogConf,
                httpHeaders);
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
