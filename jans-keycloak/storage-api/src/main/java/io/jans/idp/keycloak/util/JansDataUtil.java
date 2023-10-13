package io.jans.idp.keycloak.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JansDataUtil {

    private static final Logger logger = LoggerFactory.getLogger(JansDataUtil.class);

    public static Object invokeMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        logger.debug("JansDataUtil::invokeMethod() - Invoke clazz:{} on methodName:{} with name:{} ", clazz, methodName,
                parameterTypes);
        Object obj = null;
        if (clazz == null || methodName == null || parameterTypes == null) {
            return obj;
        }
        Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
        obj = m.invoke(null, parameterTypes);

        logger.debug("JansDataUtil::invokeMethod() - methodName:{} returned obj:{} ", methodName, obj);
        return obj;
    }

    public Object invokeReflectionGetter(Object obj, String variableName) {
        logger.debug("JansDataUtil::invokeMethod() - Invoke obj:{}, variableName:{}", obj, variableName);
        try {
            if (obj == null) {
                return obj;
            }
            PropertyDescriptor pd = new PropertyDescriptor(variableName, obj.getClass());
            Method getter = pd.getReadMethod();
            logger.debug("JansDataUtil::invokeMethod() - Invoke getter:{}", getter);
            if (getter != null) {
                return getter.invoke(obj);
            } else {
                logger.error(
                        "JansDataUtil::invokeReflectionGetter() - Getter Method not found for class:{} property:{}",
                        obj.getClass().getName(), variableName);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | IntrospectionException e) {
            logger.error(String.format(
                    "JansDataUtil::invokeReflectionGetter() - Getter Method ERROR for class: %s property: %s",
                    obj.getClass().getName(), variableName), e);
        }
        return obj;
    }

    public static List<Field> getAllFields(Class<?> type) {
        logger.debug("JansDataUtil::getAllFields() - type:{} ", type);
        List<Field> allFields = new ArrayList<>();
        if (type == null) {
            return allFields;
        }
        getAllFields(allFields, type);
        logger.debug("JansDataUtil::getAllFields() - Fields:{} of type:{}  ", allFields, type);
        return allFields;
    }

    public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        logger.debug("JansDataUtil::getAllFields() - fields:{} , type:{} ", fields, type);
        if (fields == null || type == null) {
            return fields;
        }
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
        logger.debug("JansDataUtil::getAllFields() - Final fields:{} of type:{} ", fields, type);
        return fields;
    }

    public static Map<String, String> getFieldTypeMap(Class<?> clazz) {
        logger.debug("JansDataUtil::getFieldTypeMap() - clazz:{} ", clazz);
        Map<String, String> propertyTypeMap = new HashMap<>();

        if (clazz == null) {
            return propertyTypeMap;
        }

        List<Field> fields = getAllFields(clazz);
        logger.debug("JansDataUtil::getFieldTypeMap() - all-fields:{} ", fields);

        for (Field field : fields) {
            logger.debug(
                    "JansDataUtil::getFieldTypeMap() - field:{} , field.getAnnotatedType():{}, field.getAnnotations():{} , field.getType().getAnnotations():{}, field.getType().getCanonicalName():{} , field.getType().getClass():{} , field.getType().getClasses():{} , field.getType().getComponentType():{}",
                    field, field.getAnnotatedType(), field.getAnnotations(), field.getType().getAnnotations(),
                    field.getType().getCanonicalName(), field.getType().getClass(), field.getType().getClasses(),
                    field.getType().getComponentType());
            propertyTypeMap.put(field.getName(), field.getType().getSimpleName());
        }
        logger.debug("JansDataUtil::getFieldTypeMap() - Final propertyTypeMap{} ", propertyTypeMap);
        return propertyTypeMap;
    }

}
