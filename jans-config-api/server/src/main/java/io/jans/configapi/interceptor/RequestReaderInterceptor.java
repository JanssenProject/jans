/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.core.interceptor.RequestInterceptor;

import io.jans.configapi.model.configuration.AuditLogConf;
import io.jans.configapi.util.AuthUtil;
import io.jans.orm.PersistenceEntryManager;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.jans.orm.model.AttributeData;

@Interceptor
@RequestInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RequestReaderInterceptor {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");
    private static final Logger LOG = LoggerFactory.getLogger(RequestReaderInterceptor.class);

    private static final String[] IGNORE_METHODS = { "@jakarta.ws.rs.GET()", "@jakarta.ws.rs.DELETE()",
            "@jakarta.ws.rs.OPTIONS()", "@jakarta.ws.rs.PATCH()" };

    @Context
    UriInfo info;

    @Context
    HttpServletRequest request;

    @Context
    private HttpHeaders httpHeaders;

    @Context
    private ResourceInfo resourceInfo;

    @Inject
    AuthUtil authUtil;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {
        System.out.println("\n\n\n RequestReaderInterceptor: entry -  request:{} " + request + "  info:{} " + info
                + ". resourceInfo=" + resourceInfo + " , context:{} " + context + " , persistenceEntryManager"
                + persistenceEntryManager + " \n\n\n");
        try {
            LOG.error(
                    "======================= RequestReaderInterceptor Performing DataType Conversion ============================");

            // context
            LOG.error(
                    "======RequestReaderInterceptor - context.getClass():{}, context.getConstructor(), context.getContextData():{},  context.getMethod():{},  context.getParameters():{}, context.getTarget():{}, context.getInputStream():{} ",
                    context.getClass(), context.getConstructor(), context.getContextData(), context.getMethod(),
                    context.getParameters(), context.getTarget());

            // method
            LOG.error(
                    "======RequestReaderInterceptor - context.getMethod().getAnnotatedExceptionTypes().toString() :{}, context.getMethod().getAnnotatedParameterTypes().toString() :{}, context.getMethod().getAnnotatedReceiverType().toString() :{}, context.getMethod().getAnnotation(jakarta.ws.rs.GET.class):{}, context.getMethod().getAnnotations().toString() :{}., context.getMethod().getAnnotationsByType(jakarta.ws.rs.GET.class):{} ",
                    context.getMethod().getAnnotatedExceptionTypes().toString(),
                    context.getMethod().getAnnotatedParameterTypes().toString(),
                    context.getMethod().getAnnotatedReceiverType().toString(),
                    context.getMethod().getAnnotation(jakarta.ws.rs.GET.class),
                    context.getMethod().getAnnotations().toString(),
                    context.getMethod().getAnnotationsByType(jakarta.ws.rs.GET.class));

            boolean contains = isIgnoreMethod(context);
            LOG.error("====== isIgnoreMethod:{}", contains);

            if (contains) {
                LOG.error("====== Exiting RequestReaderInterceptor as no action required for {} method. ======",
                        context.getMethod());
                return context.proceed();
            }

            processRequest(context);

        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
        return context.proceed();
    }

    private boolean isIgnoreMethod(InvocationContext context) {
        LOG.error("Checking if method to be ignored");
        if (context.getMethod().getAnnotations() == null || context.getMethod().getAnnotations().length <= 0) {
            return false;
        }

        for (int i = 0; i < context.getMethod().getAnnotations().length; i++) {
            LOG.error("======RequestReaderInterceptor - context.getMethod().getAnnotations():{} ",
                    context.getMethod().getAnnotations()[i]);

            LOG.error("======RequestReaderInterceptor - anyMatch:{} ",
                    Arrays.stream(IGNORE_METHODS).anyMatch(context.getMethod().getAnnotations()[i].toString()::equals));

            if (context.getMethod().getAnnotations()[i] != null && Arrays.stream(IGNORE_METHODS)
                    .anyMatch(context.getMethod().getAnnotations()[i].toString()::equals)) {
                LOG.error("======RequestReaderInterceptor - context.getMethod() matched and hence will be ignored!!!!");
                return true;
            }
        }
        return false;
    }

    private void processRequest(InvocationContext context) {
        LOG.error(
                "RequestReaderInterceptor Data -  context:{} , context.getClass():{}, context.getContextData():{}, context.getMethod():{} , context.getParameters():{} , context.getTarget():{} ",
                context, context.getClass(), context.getContextData(), context.getMethod(), context.getParameters(),
                context.getTarget());

        Object[] ctxParameters = context.getParameters();
        LOG.error("RequestReaderInterceptor - Processing  Data -  ctxParameters:{} ", ctxParameters);

        Method method = context.getMethod();

        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        LOG.error("RequestReaderInterceptor - Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ",
                paramCount, parameters, clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                LOG.error("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                // Audit log
                logAuditData(context, obj);

                if (!clazz.isPrimitive()) {
                    performAttributeDataConversion(obj);
                    LOG.error("RequestReaderInterceptor final - obj -  obj:{} ", obj);
                }
            }
        }
    }

    private <T> void logAuditData(InvocationContext context, T obj) {
        LOG.error("RequestReaderInterceptor -  Audit Log data  request:{}, httpHeaders:{}, resourceInfo:{} ", request,
                httpHeaders, resourceInfo);
        try {
            AuditLogConf auditLogConf = getAuditLogConf();
            LOG.error("RequestReaderInterceptor - auditLogConf:{}", auditLogConf);
            if (auditLogConf != null && auditLogConf.isEnabled()) {
                AUDIT_LOG.info("====== Request for endpoint:{}, method:{}, from:{}, user:{}, data:{} ", info.getPath(),
                        context.getMethod(), request.getRemoteAddr(), httpHeaders.getHeaderString("User-inum"), obj);
                Map<String, String> attributeMap = getAuditHeaderAttributes(auditLogConf);
                AUDIT_LOG.error("attributeMap:{} ", attributeMap);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("Exception while data conversion:{}", ex.getMessage());
        }

    }

    private <T> void performAttributeDataConversion(T obj) {
        try {
            List<AttributeData> attributes = persistenceEntryManager.getAttributesList(obj);
            AUDIT_LOG.error("RequestReaderInterceptor -  Data  for encoding -  attributes:{}", attributes);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.error("Exception while data conversion:{}", ex.getMessage());
        }

    }

    private AuditLogConf getAuditLogConf() {
        return this.authUtil.getAuditLogConf();
    }

    private Map<String, String> getAuditHeaderAttributes(AuditLogConf auditLogConf) {
        LOG.error("AuditLogConfig:{} ", auditLogConf);
        if (auditLogConf == null) {
            return Collections.emptyMap();
        }
        List<String> attributes = auditLogConf.getHeaderAttributes();
        LOG.error("AuditHeaderAttributes:{} ", attributes);
        Map<String, String> attributeMap = null;
        if (attributes != null && !attributes.isEmpty()) {
            attributeMap = new HashMap<>();
            for (String attributeName : attributes) {
                LOG.error("attributeName:{} ", attributeName);
                String attributeValue = httpHeaders.getHeaderString(attributeName);
                attributeMap.put(attributeName, attributeValue);
            }
        }
        return attributeMap;
    }

}
