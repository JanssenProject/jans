package io.jans.kc.spi.storage.util;

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

import org.jboss.logging.Logger;

public class JansDataUtil {

    private static final Logger log = Logger.getLogger(JansDataUtil.class);

    public static Object invokeMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.debugv("JansDataUtil::invokeMethod() - Invoke clazz:{0} on methodName:{1} with name:{2} ", clazz, methodName,
                parameterTypes);
        Object obj = null;
        if (clazz == null || methodName == null || parameterTypes == null) {
            return obj;
        }
        Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
        obj = m.invoke(null, parameterTypes);

        log.debugv("JansDataUtil::invokeMethod() - methodName:{0} returned obj:{1} ", methodName, obj);
        return obj;
    }

    public Object invokeReflectionGetter(Object obj, String variableName) {
        log.debugv("JansDataUtil::invokeMethod() - Invoke obj:{0}, variableName:{1}", obj, variableName);
        try {
            if (obj == null) {
                return obj;
            }
            PropertyDescriptor pd = new PropertyDescriptor(variableName, obj.getClass());
            Method getter = pd.getReadMethod();
            log.debugv("JansDataUtil::invokeMethod() - Invoke getter:{0}", getter);
            if (getter != null) {
                return getter.invoke(obj);
            } else {
                log.errorv(
                        "JansDataUtil::invokeReflectionGetter() - Getter Method not found for class:{0} property:{1}",
                        obj.getClass().getName(), variableName);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | IntrospectionException e) {
            log.errorv(e,"JansDataUtil::invokeReflectionGetter() - Getter Method ERROR for class: {0} property: {1}",
                    obj.getClass().getName(), variableName);
        }
        return obj;
    }

    public static List<Field> getAllFields(Class<?> type) {
        log.debugv("JansDataUtil::getAllFields() - type:{0} ", type);
        List<Field> allFields = new ArrayList<>();
        if (type == null) {
            return allFields;
        }
        getAllFields(allFields, type);
        log.debugv("JansDataUtil::getAllFields() - Fields:{0} of type:{1}  ", allFields, type);
        return allFields;
    }

    public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        log.debugv("JansDataUtil::getAllFields() - fields:{0} , type:{1} ", fields, type);
        if (fields == null || type == null) {
            return fields;
        }
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
        log.debugv("JansDataUtil::getAllFields() - Final fields:{0} of type:{1} ", fields, type);
        return fields;
    }

    public static Map<String, String> getFieldTypeMap(Class<?> clazz) {
        log.debugv("JansDataUtil::getFieldTypeMap() - clazz:{0} ", clazz);
        Map<String, String> propertyTypeMap = new HashMap<>();

        if (clazz == null) {
            return propertyTypeMap;
        }

        List<Field> fields = getAllFields(clazz);
        log.debugv("JansDataUtil::getFieldTypeMap() - all-fields:{0} ", fields);

        for (Field field : fields) {
            log.debugv(
                    "JansDataUtil::getFieldTypeMap() - field:{0} , field.getAnnotatedType():{1}, field.getAnnotations():{2} , field.getType().getAnnotations():{3}, field.getType().getCanonicalName():{4} , field.getType().getClass():{5} , field.getType().getClasses():{6} , field.getType().getComponentType():{7}",
                    field, field.getAnnotatedType(), field.getAnnotations(), field.getType().getAnnotations(),
                    field.getType().getCanonicalName(), field.getType().getClass(), field.getType().getClasses(),
                    field.getType().getComponentType());
            propertyTypeMap.put(field.getName(), field.getType().getSimpleName());
        }
        log.debugv("JansDataUtil::getFieldTypeMap() - Final propertyTypeMap{0} ", propertyTypeMap);
        return propertyTypeMap;
    }

}
