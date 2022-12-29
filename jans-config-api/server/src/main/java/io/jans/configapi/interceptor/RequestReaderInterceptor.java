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
import io.jans.orm.annotation.AttributesList;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import jakarta.ws.rs.core.UriInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.jans.orm.model.AttributeData;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.util.ReflectHelper;

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
        logger.error(
                "\n\n\n RequestReaderInterceptor: entry -  info:{}, request:{}, httpHeaders:{}, resourceInfo:{}, persistenceEntryManager:{}",
                info, request, httpHeaders, resourceInfo, persistenceEntryManager);
        try {
            logger.error(
                    "======================= RequestReaderInterceptor Performing DataType Conversion ============================");

            // context
            logger.error(
                    "======RequestReaderInterceptor - context.getClass():{}, context.getConstructor(), context.getContextData():{},  context.getMethod():{},  context.getParameters():{}, context.getTarget():{}, context.getInputStream():{} ",
                    context.getClass(), context.getConstructor(), context.getContextData(), context.getMethod(),
                    context.getParameters(), context.getTarget());

            // method
            logger.error(
                    "======RequestReaderInterceptor - context.getMethod().getAnnotatedExceptionTypes().toString() :{}, context.getMethod().getAnnotatedParameterTypes().toString() :{}, context.getMethod().getAnnotatedReceiverType().toString() :{}, context.getMethod().getAnnotation(jakarta.ws.rs.GET.class):{}, context.getMethod().getAnnotations().toString() :{}., context.getMethod().getAnnotationsByType(jakarta.ws.rs.GET.class):{} ",
                    context.getMethod().getAnnotatedExceptionTypes().toString(),
                    context.getMethod().getAnnotatedParameterTypes().toString(),
                    context.getMethod().getAnnotatedReceiverType().toString(),
                    context.getMethod().getAnnotation(jakarta.ws.rs.GET.class),
                    context.getMethod().getAnnotations().toString(),
                    context.getMethod().getAnnotationsByType(jakarta.ws.rs.GET.class));

            boolean contains = isIgnoreMethod(context);
            logger.error("====== isIgnoreMethod:{}", contains);

            if (contains) {
                logger.error("====== Exiting RequestReaderInterceptor as no action required for {} method. ======",
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
        logger.error("Checking if method to be ignored");
        if (context.getMethod().getAnnotations() == null || context.getMethod().getAnnotations().length <= 0) {
            return false;
        }

        for (int i = 0; i < context.getMethod().getAnnotations().length; i++) {
            logger.error("======RequestReaderInterceptor - context.getMethod().getAnnotations():{} ",
                    context.getMethod().getAnnotations()[i]);

            logger.error("======RequestReaderInterceptor - anyMatch:{} ",
                    Arrays.stream(IGNORE_METHODS).anyMatch(context.getMethod().getAnnotations()[i].toString()::equals));

            if (context.getMethod().getAnnotations()[i] != null && Arrays.stream(IGNORE_METHODS)
                    .anyMatch(context.getMethod().getAnnotations()[i].toString()::equals)) {
                logger.error(
                        "======RequestReaderInterceptor - context.getMethod() matched and hence will be ignored!!!!");
                return true;
            }
        }
        return false;
    }

    private void processRequest(InvocationContext context) {
        logger.error(
                "RequestReaderInterceptor Data -  context:{} , context.getClass():{}, context.getContextData():{}, context.getMethod():{} , context.getParameters():{} , context.getTarget():{} ",
                context, context.getClass(), context.getContextData(), context.getMethod(), context.getParameters(),
                context.getTarget());
        logger.error(
                "RequestReaderInterceptor Data -  context:{} , context.getClass():{}, context.getContextData():{}, context.getMethod():{} , context.getParameters():{} , context.getTarget():{} ",
                context, context.getClass(), context.getContextData(), context.getMethod(), context.getParameters(),
                context.getTarget());

        Object[] ctxParameters = context.getParameters();
        logger.error("RequestReaderInterceptor - Processing  Data -  ctxParameters:{} ", ctxParameters);

        Method method = context.getMethod();

        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        logger.error("RequestReaderInterceptor - Processing  Data -  paramCount:{} , parameters:{}, clazzArray:{} ",
                paramCount, parameters, clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                logger.error("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                if (!clazz.isPrimitive()) {
                    //performAttributeDataConversion(obj);
                    processCustomAttributes(obj);
                    logger.error("RequestReaderInterceptor final - obj -  obj:{} ", obj);
                }
            }
        }
    }

    private <T> void performAttributeDataConversion(T obj) {
        List<AttributeData> attributes = persistenceEntryManager.getAttributeDataList(obj);
        logger.error("RequestReaderInterceptor::Attribute List  for encoding -  obj.getClass():{}, attributes:{}",
                obj.getClass(), attributes);

        // get attribute details
        if (attributes != null && !attributes.isEmpty()) {
            for (AttributeData attData : attributes) {
                logger.error("RequestReaderInterceptor::AttributeData  for encoding -  attData:{}", attData);
                GluuAttribute gluuAttribute = attributeService.getByLdapName(attData.getName());
                AttributeData modifiedData = null;
                logger.error("RequestReaderInterceptor::Attribute details - attData.getName():{}, gluuAttribute:{}",
                        attData.getName(), gluuAttribute);
                if (gluuAttribute != null) {
                    AttributeDataType attributeDataType = gluuAttribute.getDataType();
                    logger.error(" attributeDataType:{}", attributeDataType);
                    if (attributeDataType != null && AttributeDataType.DATE.equals(attributeDataType)) {
                        if (attributeDataType.getValue() != null) {
                            Date date = persistenceEntryManager.decodeTime(null, attributeDataType.getValue());
                            logger.error(" attributeDataType.getDisplayName():{}, date:{}",
                                    attributeDataType.getDisplayName(), date);
                            
                            modifiedData = new AttributeData(attributeDataType.getDisplayName(), date);
                            logger.error("RequestReaderInterceptor::AttributeData  after encoding -  attData:{}, modifiedData:{}", attData, modifiedData);
                        }
                    }
                }

            }
        }

    }

    private <T> void processCustomAttributes(T obj) {
        logger.error("RequestReaderInterceptor::processCustomAttributes() obj:{}", obj);
        //
        Class<?> entryClass = obj.getClass();
        List<PropertyAnnotation> propertiesAnnotations = persistenceEntryManager
                .getEntryPropertyAnnotations(entryClass);
        logger.error("RequestReaderInterceptor::processCustomAttributes() -  propertiesAnnotations:{}", propertiesAnnotations);

        for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
            String propertyName = propertiesAnnotation.getPropertyName();

            // Process properties with @AttributesList annotation
            Annotation ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
                    AttributesList.class);
            logger.error("RequestReaderInterceptor::processCustomAttributes() - AttributesList - ldapAttribute:{}",
                    ldapAttribute);
            if (ldapAttribute != null) {
                List<AttributeData> listAttributes = persistenceEntryManager
                        .getAttributeDataListFromCustomAttributesList(obj, (AttributesList) ldapAttribute,
                                propertyName);
                logger.error("RequestReaderInterceptor::processCustomAttributes() - AttributesList - listAttributes:{}",
                        listAttributes);
                if (listAttributes != null && !listAttributes.isEmpty()) {
                    for (AttributeData attData : listAttributes) {
                        logger.error(
                                "RequestReaderInterceptor::processCustomAttributes() - AttributeData  for encoding -  attData:{}",
                                attData);
                        GluuAttribute gluuAttribute = attributeService.getByLdapName(attData.getName());
                        logger.error(
                                "RequestReaderInterceptor::Attribute details - attData.getName():{}, gluuAttribute:{}",
                                attData.getName(), gluuAttribute);
                        if (gluuAttribute != null) {
                            AttributeDataType attributeDataType = gluuAttribute.getDataType();
                            logger.error("RequestReaderInterceptor::processCustomAttributes() - attributeDataType:{}",
                                    attributeDataType);
                            if (attributeDataType != null && AttributeDataType.DATE.equals(attributeDataType)) {
                                if (attributeDataType.getValue() != null) {
                                    Date date = persistenceEntryManager.decodeTime(null, attributeDataType.getValue());
                                    logger.error(
                                            "RequestReaderInterceptor::processCustomAttributes() - attributeDataType.getDisplayName():{}, date:{}",
                                            attributeDataType.getDisplayName(), date);
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}
