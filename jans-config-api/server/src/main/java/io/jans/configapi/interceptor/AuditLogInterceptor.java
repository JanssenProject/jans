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
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
            AUDIT_LOG.info("\n ********************** Request Details ********************** ");

            // Get Audit config
            AuditLogConf auditLogConf = getAuditLogConf();
            LOG.info("auditLogConf:{}", auditLogConf);

            // Log if enabled
            if (ignoreMethod()) {

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
        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        AUDIT_LOG.debug("RequestReaderInterceptor - Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ",
                paramCount, parameters, clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                AUDIT_LOG.debug("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                AUDIT_LOG.debug("RequestReaderInterceptor final - obj -  obj:{} ", obj);

            }
        }
    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }
    
    private boolean ignoreMethod() {
       
        LOG.debug("request.getMethod():{}, getAuditLogConf().getIgnoreHttpMethod():{}, getAuditLogConf().getIgnoreHttpMethod().contains(request.getMethod()):{}", request.getMethod() ,getAuditLogConf().getIgnoreHttpMethod(), getAuditLogConf().getIgnoreHttpMethod().contains(request.getMethod()));
        if(getAuditLogConf()!=null && getAuditLogConf().getIgnoreHttpMethod()!=null && getAuditLogConf().getIgnoreHttpMethod().contains(request.getMethod())) {
            return true;
        }
        
        return false;
        
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
