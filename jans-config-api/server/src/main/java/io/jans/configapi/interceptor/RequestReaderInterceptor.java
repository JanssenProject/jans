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
import java.util.Date;
import java.util.List;

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
        logger.error(
                "\n\n RequestReaderInterceptor::aroundReadFrom() - Request Interceptor info:{}, request:{}, httpHeaders:{}, resourceInfo:{}, persistenceEntryManager:{}, getDataFormatConversionConf():{}",
                info, request, httpHeaders, resourceInfo, persistenceEntryManager, getDataFormatConversionConf());
        logger.error(
                "Request Interceptor info:{}, request:{}, httpHeaders:{}, resourceInfo:{}, persistenceEntryManager:{}",
                info, request, httpHeaders, resourceInfo, persistenceEntryManager);
        try {
            // perform data conversion if enabled and method is not ignored
            if (isDataFormatConversionEnaled() || !ignoreMethod(context)) {
                logger.error("=======================  DataType Conversion Start ============================");
                processRequest(context);
                logger.error("=======================  DataType Conversion End ============================");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Exception while data conversion:{}", ex.getMessage());
        }
        return context.proceed();
    }

    private void processRequest(InvocationContext context) {
        logger.error(
                " Process Request Data -  context:{} , context.getClass():{}, context.getContextData():{}, context.getMethod():{} , context.getParameters():{} , context.getTarget():{} ",
                context, context.getClass(), context.getContextData(), context.getMethod(), context.getParameters(),
                context.getTarget());

        Object[] ctxParameters = context.getParameters();
        logger.error(" Request  Parameters -  ctxParameters:{} ", ctxParameters);

        Method method = context.getMethod();

        int paramCount = method.getParameterCount();
        Parameter[] parameters = method.getParameters();
        Class[] clazzArray = method.getParameterTypes();

        logger.error("Parameter  Data -  paramCount:{} , parameters:{}, clazzArray:{} ", paramCount, parameters,
                clazzArray);

        if (clazzArray != null && clazzArray.length > 0) {
            for (int i = 0; i < clazzArray.length; i++) {
                Class<?> clazz = clazzArray[i];
                String propertyName = parameters[i].getName();
                logger.error("propertyName:{}, clazz:{} , clazz.isPrimitive():{} ", propertyName, clazz,
                        clazz.isPrimitive());

                Object obj = ctxParameters[i];
                if (!clazz.isPrimitive()) {
                    processCustomAttributes(obj);
                    logger.error("Request object post processing -  propertyName:{}, obj:{} ", propertyName, obj);
                }
            }
        }
    }

    private <T> void processCustomAttributes(T obj) {
        logger.error("Object for custom attribute obj:{}", obj);

        Class<?> entryClass = obj.getClass();
        List<PropertyAnnotation> propertiesAnnotations = persistenceEntryManager
                .getEntryPropertyAnnotations(entryClass);
        logger.error("propertiesAnnotations:{}", propertiesAnnotations);

        for (PropertyAnnotation propertiesAnnotation : propertiesAnnotations) {
            try {
                String propertyName = propertiesAnnotation.getPropertyName();

                // Process properties with @AttributesList annotation
                Annotation ldapAttribute = ReflectHelper.getAnnotationByType(propertiesAnnotation.getAnnotations(),
                        AttributesList.class);
                logger.error("Custom attributes - ldapAttribute:{}", ldapAttribute);
                if (ldapAttribute == null) {
                    continue;
                }

                List<AttributeData> listAttributes = persistenceEntryManager
                        .getAttributeDataListFromCustomAttributesList(obj, (AttributesList) ldapAttribute,
                                propertyName);
                logger.error("Custom AttributesList before conversion listAttributes:{}", listAttributes);
                processAttributeData(obj, propertyName, ldapAttribute, listAttributes);
                logger.error("Custom AttributesList after conversion listAttributes:{}", listAttributes);

            } catch (Exception ex) {
                logger.error("Error while processing Custom Attributes", ex);
            }
        }
    }

    private List<AttributeData> processAttributeData(Object obj, String propertyName, Annotation ldapAttribute,
            List<AttributeData> listAttributes)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.error("Attribute Data for processing obj:{}, propertyName:{}, ldapAttribute:{}, listAttributes:{}", obj,
                propertyName, ldapAttribute, listAttributes);

        if (listAttributes != null && !listAttributes.isEmpty()) {
            for (AttributeData attData : listAttributes) {
                logger.error("AttributeData - attData:{}", attData);
                GluuAttribute gluuAttribute = attributeService.getByLdapName(attData.getName());

                logger.error("AttributeData details - attData.getName():{}, attData.getValue():{},gluuAttribute:{}",
                        attData.getName(), attData.getValue(), gluuAttribute);

                if (attData.getValue() != null && gluuAttribute != null) {
                    AttributeDataType attributeDataType = gluuAttribute.getDataType();
                    logger.error(
                            "AttributeDataType - attData.getName():{}, attributeDataType:{}, AttributeDataType.DATE.getValue():{}",
                            attData.getName(), attributeDataType, AttributeDataType.DATE.getValue());
                    if (AttributeDataType.DATE.getValue().equalsIgnoreCase(attributeDataType.getValue())) {
                        logger.error(" Calling decodeTime() - attData.getValue():{}", attData.getValue());
                        AttributeData attributeData = decodeTime(attData);
                        listAttributes.remove(attData);
                        listAttributes.add(attributeData);
                    }
                }
            }

            logger.error("Getting updated custom attribute list for propertyName:{} , listAttributes:{} ", propertyName,
                    listAttributes);
            List<Object> data = persistenceEntryManager.getCustomAttributesListFromAttributeDataList(obj,
                    (AttributesList) ldapAttribute, propertyName, listAttributes);
            logger.error("updated custom attribute data:{}", data);

            logger.error("Setting the custom attribute in request object propertyName:{}, data:{} ", propertyName,
                    data);
            // set data
            setObjectData(obj, propertyName, data);
            logger.error("After setting the custom attribute in request object propertyName:{}, data:{} ", propertyName,
                    data);

        }
        return listAttributes;
    }

    private AttributeData decodeTime(AttributeData attributeData) {
        logger.error("Date data to decode attributeData:{}", attributeData);
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
                    logger.error("atrData.getName():{}, date:{}", atrData.getName(), date);
                    atrData = new AttributeData(atrData.getName(), date);
                    atrData.setMultiValued(attributeData.getMultiValued());
                }

            }
        }
        return atrData;
    }

    private void setObjectData(Object obj, String propertyName, Object propertyValue)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        logger.error("Data to set new value - obj:{}, propertyName:{}, propertyValue:{}", obj, propertyName,
                propertyValue);
        Setter setterMethod = DataUtil.getSetterMethod(obj.getClass(), propertyName);
        propertyValue = setterMethod.getMethod().invoke(obj, propertyValue);
        logger.error("After setterMethod invoked key:{}, propertyValue:{} ", propertyName, propertyValue);

        Getter getterMethod = DataUtil.getGetterMethod(obj.getClass(), propertyName);
        propertyValue = getterMethod.get(obj);
        logger.error("Verify new value key:{}, propertyValue:{} ", propertyName, propertyValue);

    }

    private DataFormatConversionConf getDataFormatConversionConf() {
        logger.error("authUtil.getDataFormatConversionConf():{}", authUtil.getDataFormatConversionConf());
        return this.authUtil.getDataFormatConversionConf();
    }

    private boolean isDataFormatConversionEnaled() {
        if (getDataFormatConversionConf() == null) {
            return false;
        }
        logger.error("getDataFormatConversionConf().isEnabled():{}",
                getDataFormatConversionConf().isEnabled());
        return getDataFormatConversionConf().isEnabled();
    }

    private boolean ignoreMethod(InvocationContext context) {
        logger.error(
                "context.getMethod():{}, getDataFormatConversionConf().getIgnoreHttpMethod():{}, getDataFormatConversionConf().getIgnoreHttpMethod().contains(context.getMethod()):{}",
                context.getMethod(), getDataFormatConversionConf().getIgnoreHttpMethod(),
                getDataFormatConversionConf().getIgnoreHttpMethod().contains(context.getMethod()));
        boolean ignoreFlag = false;
        if (getDataFormatConversionConf() != null && getDataFormatConversionConf().getIgnoreHttpMethod() != null
                && getDataFormatConversionConf().getIgnoreHttpMethod().contains(context.getMethod())) {
            ignoreFlag = true;
        }

        return ignoreFlag;

    }

}
