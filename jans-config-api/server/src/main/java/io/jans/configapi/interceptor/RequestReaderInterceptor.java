/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.interceptor;

import io.jans.model.GluuAttribute;
import io.jans.model.attribute.AttributeDataType;
import io.jans.configapi.core.util.DataUtil;
import io.jans.configapi.model.configuration.DataFormatConversionConf;
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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import io.jans.orm.model.AttributeData;
import io.jans.orm.reflect.property.Getter;
import io.jans.orm.reflect.property.PropertyAnnotation;
import io.jans.orm.reflect.property.Setter;
import io.jans.orm.reflect.util.ReflectHelper;

import java.lang.reflect.InvocationTargetException;

@Interceptor
@RequestInterceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RequestReaderInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestReaderInterceptor.class);

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
        logger.debug("Request Interceptor  info:{}, request:{}, httpHeaders:{}, resourceInfo:{}, persistenceEntryManager:{}",
                info, request, httpHeaders, resourceInfo, persistenceEntryManager);
        try {
            logger.debug(
                    "=======================  DataType Conversion ============================");
            logger.error(" RequestReaderInterceptor: entry - request.getMethod():{}, context.getMethod():{}, isDataFormatConversionEnaled():{},  ignoreMethod():{} ", request.getMethod(), context.getMethod(), isDataFormatConversionEnaled(), ignoreMethod());
            
            //Exit if data conversion if enabled and method is not be ignored            
            if(!isDataFormatConversionEnaled() || ignoreMethod() ) {
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

   

    private void processRequest(InvocationContext context) {
        logger.debug(" Process Request Data -  context:{} , context.getClass():{}, context.getContextData():{}, context.getMethod():{} , context.getParameters():{} , context.getTarget():{} ",
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
                    processCustomAttributes(obj);
                    logger.debug("RequestReaderInterceptor final - obj -  obj:{} ", obj);
                }
            }
        }
    }

    private <T> void processCustomAttributes(T obj) {
        logger.debug("RequestReaderInterceptor::processCustomAttributes() obj:{}", obj);
        //
        Class<?> entryClass = obj.getClass();
        List<PropertyAnnotation> propertiesAnnotations = persistenceEntryManager
                .getEntryPropertyAnnotations(entryClass);
        logger.debug("RequestReaderInterceptor::processCustomAttributes() -  propertiesAnnotations:{}",
                propertiesAnnotations);

        for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
            try {
                String propertyName = propertiesAnnotation.getPropertyName();

                // Process properties with @AttributesList annotation
                Annotation ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
                        AttributesList.class);
                logger.debug("RequestReaderInterceptor::processCustomAttributes() - AttributesList - ldapAttribute:{}",
                        ldapAttribute);
                if (ldapAttribute == null) {
                    continue;
                }

                List<AttributeData> listAttributes = persistenceEntryManager
                        .getAttributeDataListFromCustomAttributesList(obj, (AttributesList) ldapAttribute,
                                propertyName);
                logger.debug("RequestReaderInterceptor::processCustomAttributes() - AttributesList - listAttributes:{}",
                        listAttributes);
                
                if (listAttributes != null && !listAttributes.isEmpty()) {
                    for (AttributeData attData : listAttributes) {
                        logger.debug("RequestReaderInterceptor::processCustomAttributes() - attData:{}", attData);
                        GluuAttribute gluuAttribute = attributeService.getByLdapName(attData.getName());

                        logger.debug(
                                "RequestReaderInterceptor::Attribute details - attData.getName():{}, attData.getValue():{},gluuAttribute:{}",
                                attData.getName(), attData.getValue(), gluuAttribute);
                        
                        if (attData != null && attData.getValue() != null && gluuAttribute != null) {
                            AttributeDataType attributeDataType = gluuAttribute.getDataType();
                            logger.debug("RequestReaderInterceptor::processCustomAttributes() - attributeDataType:{}, AttributeDataType.DATE.getValue():{}",
                                    attributeDataType, AttributeDataType.DATE.getValue());
                            if (AttributeDataType.DATE.getValue().equalsIgnoreCase(attributeDataType.getValue())) {
                                logger.debug("RequestReaderInterceptor::processCustomAttributes() - Calling decodeTime() - attData.getValue():{}", attData.getValue());
                                AttributeData attributeData = decodeTime(attData);                                
                                listAttributes.remove(attData);
                                listAttributes.add(attributeData);
                            }
                        }
                    }
                    
                    logger.debug("RequestReaderInterceptor::processCustomAttributes() - calling getCustomAttributesListFromAttributeDataList() - propertyName:{} , listAttributes:{} ", propertyName, listAttributes);
                    List<Object> data = persistenceEntryManager.getCustomAttributesListFromAttributeDataList(obj,  (AttributesList) ldapAttribute, propertyName, listAttributes);
                    logger.debug("RequestReaderInterceptor::processCustomAttributes() - data:{}", data);
                    
                    
                    logger.debug("RequestReaderInterceptor::processCustomAttributes() - before calling calling setObjectData() - propertyName:{}, data:{} ", propertyName, data);
                    //set data
                    setObjectData(obj, propertyName, data);
                    logger.debug("RequestReaderInterceptor::processCustomAttributes() - after calling setObjectData() - propertyName:{}, data:{} ", propertyName, data);
                    
                    
                }
                
                

            } catch (Exception ex) {
                logger.debug("Error while processing Custom Attributes", ex);
            }
        }
    }

    private AttributeData decodeTime(AttributeData attributeData) {
        logger.debug("RequestReaderInterceptor::decodeTime() date - attributeData:{}", attributeData);
        if (attributeData == null) {
            return attributeData;
        }
        AttributeData atrData = attributeData;
        if (atrData.getValue() != null) {
            Object attValue = atrData.getValue();
            if (attValue != null) {
                Date date = authUtil.parseStringToDateObj(attValue.toString());                
                if (date == null) {
                    date = persistenceEntryManager.decodeTime(null, attValue.toString());
                    logger.error(
                            "RequestReaderInterceptor::decodeTime() - atrData.getName():{}, date:{}",
                            atrData.getName(), date);
                    atrData = new AttributeData(atrData.getName(), date);
                    atrData.setMultiValued(attributeData.getMultiValued());
                }
                
                
            }
        }
        return atrData;
    }
    
    private void setObjectData(Object obj, String propertyName, Object propertyValue) throws IllegalAccessException, IllegalArgumentException,
    InvocationTargetException {
        logger.debug("RequestReaderInterceptor::setObjectData() - obj:{}, propertyName:{},propertyValue:{}", obj, propertyValue);
        Setter setterMethod = DataUtil.getSetterMethod(obj.getClass(), propertyName);
        propertyValue = setterMethod.getMethod().invoke(obj, propertyValue);
        logger.debug("RequestReaderInterceptor::setObjectData() - After setterMethod invoked key:{}, propertyValue:{} ", propertyName, propertyValue);

        Getter getterMethod = DataUtil.getGetterMethod(obj.getClass(), propertyName);
        logger.debug("RequestReaderInterceptor::setObjectData() - propertyName:{}, getterMethod:{} ", propertyName, getterMethod);

        propertyValue = getterMethod.get(obj);
        logger.debug("Final RequestReaderInterceptor::setObjectData() - key:{}, propertyValue:{} ", propertyName, propertyValue);
       
   
    }
    
    private DataFormatConversionConf getDataFormatConversionConf() {
        logger.debug("authUtil.getDataFormatConversionConf():{}", authUtil.getDataFormatConversionConf());
        return this.authUtil.getDataFormatConversionConf();
    }

    private boolean isDataFormatConversionEnaled() {
        if(getDataFormatConversionConf() == null) {
            return false;
        }
        logger.debug("authUtil.getDataFormatConversionConf().isEnabled():{}", authUtil.getDataFormatConversionConf().isEnabled());
        return getDataFormatConversionConf().isEnabled();
    }
    
    private boolean ignoreMethod() {
        
        logger.debug("request.getMethod():{}, getDataFormatConversionConf().getIgnoreHttpMethod():{}, getDataFormatConversionConf().getIgnoreHttpMethod().contains(request.getMethod()):{}", request.getMethod() ,getDataFormatConversionConf().getIgnoreHttpMethod(), getDataFormatConversionConf().getIgnoreHttpMethod().contains(request.getMethod()));
        if(getDataFormatConversionConf()!=null && getDataFormatConversionConf().getIgnoreHttpMethod()!=null && getDataFormatConversionConf().getIgnoreHttpMethod().contains(request.getMethod())) {
            return true;
        }
        
        return false;
        
    }

   
}
