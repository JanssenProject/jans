/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import io.jans.model.GluuAttribute;
import io.jans.model.attribute.AttributeDataType;
import io.jans.configapi.core.interceptor.RequestInterceptor;
import io.jans.configapi.service.auth.AttributeService;
import io.jans.configapi.util.AuthUtil;
import io.jans.orm.PersistenceEntryManager;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import jakarta.ws.rs.core.UriInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

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

    private static final Logger logger = LoggerFactory.getLogger(RequestReaderInterceptor.class);
    private static final String[] IGNORE_METHODS = {};

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
    AttributeService attributeService;

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @SuppressWarnings({ "all" })
    @AroundInvoke
    public Object aroundReadFrom(InvocationContext context) throws Exception {
        logger.debug(
                "\n\n\n RequestReaderInterceptor: entry -  info:{}, request:{}, httpHeaders:{}, resourceInfo:{}, persistenceEntryManager:{}",
                info, request, httpHeaders, resourceInfo, persistenceEntryManager);
        try {
            logger.debug(
                    "======================= RequestReaderInterceptor Performing DataType Conversion ============================");

            // context
            logger.debug(
                    "======RequestReaderInterceptor - context.getClass():{}, context.getConstructor(), context.getContextData():{},  context.getMethod():{},  context.getParameters():{}, context.getTarget():{}, context.getInputStream():{} ",
                    context.getClass(), context.getConstructor(), context.getContextData(), context.getMethod(),
                    context.getParameters(), context.getTarget());

            // method
            logger.debug(
                    "======RequestReaderInterceptor - context.getMethod().getAnnotatedExceptionTypes().toString() :{}, context.getMethod().getAnnotatedParameterTypes().toString() :{}, context.getMethod().getAnnotatedReceiverType().toString() :{}, context.getMethod().getAnnotation(jakarta.ws.rs.GET.class):{}, context.getMethod().getAnnotations().toString() :{}., context.getMethod().getAnnotationsByType(jakarta.ws.rs.GET.class):{} ",
                    context.getMethod().getAnnotatedExceptionTypes().toString(),
                    context.getMethod().getAnnotatedParameterTypes().toString(),
                    context.getMethod().getAnnotatedReceiverType().toString(),
                    context.getMethod().getAnnotation(jakarta.ws.rs.GET.class),
                    context.getMethod().getAnnotations().toString(),
                    context.getMethod().getAnnotationsByType(jakarta.ws.rs.GET.class));

            boolean contains = isIgnoreMethod(context);
            logger.debug("====== isIgnoreMethod:{}", contains);

            if (contains) {
                logger.debug("====== Exiting RequestReaderInterceptor as no action required for {} method. ======",
                        context.getMethod());
                return context.proceed();
            }

            processRequest(context);

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception while data conversion:{}", ex.getMessage());
        }
        return context.proceed();
    }

    private boolean isIgnoreMethod(InvocationContext context) {
        logger.debug("Checking if method to be ignored");
        if (context.getMethod().getAnnotations() == null || context.getMethod().getAnnotations().length <= 0) {
            return false;
        }

        for (int i = 0; i < context.getMethod().getAnnotations().length; i++) {
            logger.debug("======RequestReaderInterceptor - context.getMethod().getAnnotations():{} ",
                    context.getMethod().getAnnotations()[i]);

            logger.debug("======RequestReaderInterceptor - anyMatch:{} ",
                    Arrays.stream(IGNORE_METHODS).anyMatch(context.getMethod().getAnnotations()[i].toString()::equals));

            if (context.getMethod().getAnnotations()[i] != null && Arrays.stream(IGNORE_METHODS)
                    .anyMatch(context.getMethod().getAnnotations()[i].toString()::equals)) {
                logger.debug(
                        "======RequestReaderInterceptor - context.getMethod() matched and hence will be ignored!!!!");
                return true;
            }
        }
        return false;
    }

    private void processRequest(InvocationContext context) {
        logger.debug(
                "RequestReaderInterceptor Data -  context:{} , context.getClass():{}, context.getContextData():{}, context.getMethod():{} , context.getParameters():{} , context.getTarget():{} ",
                context, context.getClass(), context.getContextData(), context.getMethod(), context.getParameters(),
                context.getTarget());
        logger.debug(
                "RequestReaderInterceptor Data -  context:{} , context.getClass():{}, context.getContextData():{}, context.getMethod():{} , context.getParameters():{} , context.getTarget():{} ",
                context, context.getClass(), context.getContextData(), context.getMethod(), context.getParameters(),
                context.getTarget());

        Object[] ctxParameters = context.getParameters();
        logger.debug("RequestReaderInterceptor - Processing  Data -  ctxParameters:{} ", ctxParameters);

        Method method = context.getMethod();

        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        logger.debug("RequestReaderInterceptor - Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ",
                paramCount, parameters, clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                logger.debug("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                if (!clazz.isPrimitive()) {
                    performAttributeDataConversion(obj);
                    logger.debug("RequestReaderInterceptor final - obj -  obj:{} ", obj);
                }
            }
        }
    }

    private <T> void performAttributeDataConversion(T obj) {
        List<AttributeData> attributes = persistenceEntryManager.getAttributesList(obj);
        logger.debug("Attribute List  for encoding -  obj.getClass():{}, attributes:{}", obj.getClass(), attributes);
        
        //get attribute details
        if(attributes!=null && !attributes.isEmpty()) {
            for(AttributeData attData : attributes) {
                logger.debug("AttributeData  for encoding -  attData:{}", attData);
                GluuAttribute gluuAttribute = attributeService.getAttributeUsingName(attData.getName());
                logger.debug("Attribute details  gluuAttribute:{}", gluuAttribute);
                if(gluuAttribute!=null) {
                    AttributeDataType attributeDataType = gluuAttribute.getDataType();
                    logger.debug(" attributeDataType:{}", attributeDataType);
                    if (AttributeDataType.DATE.equals(attributeDataType)) {
                       /* final Object value = user.getAttribute(gluuAttribute.getName(), true, gluuAttribute.getOxMultiValuedAttribute());
                        if (value instanceof Date) {
                            attribute = value;
                        } else if (value != null) {
                            attribute = entryManager.decodeTime(user.getDn(), value.toString());
                        }*/
                    }
                }
                
            }
            
        }

    }

}
