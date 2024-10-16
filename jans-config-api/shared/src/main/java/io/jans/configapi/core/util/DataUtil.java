package io.jans.configapi.core.util;

import io.jans.as.model.json.JsonApplier;
import io.jans.model.FieldFilterData;
import io.jans.model.FilterOperator;
import io.jans.model.attribute.AttributeDataType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.MappingException;
import io.jans.orm.reflect.property.Getter;
import io.jans.orm.reflect.property.Setter;
import io.jans.orm.reflect.util.ReflectHelper;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Named("dataUtil")
public class DataUtil {

    private static final Logger logger = LoggerFactory.getLogger(DataUtil.class);

    public static Class<?> getPropertType(String className, String name) throws MappingException {
        logger.debug("className:{} , name:{} ", className, name);
        return ReflectHelper.reflectedPropertyClass(className, name);
    }

    public static Getter getGetterMethod(Class<?> clazz, String name) throws MappingException {
        logger.debug("Get Getter fromclazz:{} , name:{} ", clazz, name);
        return ReflectHelper.getGetter(clazz, name);
    }

    public static Setter getSetterMethod(Class<?> clazz, String name) throws MappingException {
        logger.debug("Get Setter from clazz:{} for name:{} ", clazz, name);
        return ReflectHelper.getSetter(clazz, name);
    }

    public static Object getValue(Object object, String property) throws MappingException {
        logger.debug("Get value from object:{} for property:{} ", object, property);
        return ReflectHelper.getValue(object, property);
    }

    public static Method getSetter(String fieldName, Class<?> clazz) throws IntrospectionException {
        PropertyDescriptor[] props = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        for (PropertyDescriptor p : props)
            if (p.getName().equals(fieldName))
                return p.getWriteMethod();
        return null;
    }

    public static Object invokeMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        logger.debug("Invoke clazz:{} on methodName:{} with name:{} ", clazz, methodName, parameterTypes);
        Method m = clazz.getDeclaredMethod(methodName, parameterTypes);
        Object obj = m.invoke(null, parameterTypes);
        logger.debug("methodName:{} returned obj:{} ", methodName, obj);
        return obj;
    }

    public Object invokeReflectionGetter(Object obj, String variableName) {
        try {
            PropertyDescriptor pd = new PropertyDescriptor(variableName, obj.getClass());
            Method getter = pd.getReadMethod();
            if (getter != null) {
                return getter.invoke(obj);
            } else {
                logger.error("Getter Method not found for class:{} property:{}", obj.getClass().getName(),
                        variableName);
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | IntrospectionException e) {
            logger.error(String.format("Getter Method ERROR for class: %s property: %s", obj.getClass().getName(),
                    variableName), e);
        }
        return null;
    }

    public static void invokeReflectionSetter(Object obj, String propertyName, Object variableValue) {
        PropertyDescriptor pd;
        try {
            pd = new PropertyDescriptor(propertyName, obj.getClass());
            Method method = pd.getWriteMethod();
            if (method != null) {
                method.invoke(obj, variableValue);
            } else {
                logger.error("Setter Method not found for class:{} property:{}", obj.getClass().getName(),
                        propertyName);
            }
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            logger.error("Setter Method invocation ERROR for class:{} property:{}", obj.getClass().getName(),
                    propertyName, e);
        }
    }

    public static boolean containsField(List<Field> allFields, String attribute) {
        logger.debug("allFields:{},  attribute:{}, allFields.contains(attribute):{} ", allFields, attribute,
                allFields.stream().anyMatch(f -> f.getName().equals(attribute)));

        return allFields.stream().anyMatch(f -> f.getName().equals(attribute));
    }

    public boolean isStringField(Map<String, String> objectPropertyMap, String attribute) {
        logger.debug("Check if field is string objectPropertyMap:{}, attribute:{} ", objectPropertyMap, attribute);
        if (objectPropertyMap == null || StringUtils.isBlank(attribute)) {
            return false;
        }
        logger.debug("attribute:{} , datatype:{}", attribute, objectPropertyMap.get(attribute));
        return ("java.lang.String".equalsIgnoreCase(objectPropertyMap.get(attribute)));
    }

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> allFields = new ArrayList<>();
        getAllFields(allFields, type);
        logger.debug("Fields:{} of type:{}  ", allFields, type);
        return allFields;
    }

    public static Map<String, String> getFieldDataType(Class<?> type, List<String> fieldList) {
        logger.error("getFieldDataType - type:{} , fieldList:{}  ", type, fieldList);
        Map<String, String> fieldTypeMap = null;
        if (type == null || fieldList == null || fieldList.isEmpty()) {
            return fieldTypeMap;
        }

        fieldTypeMap = getFieldTypeMap(type);
        if (fieldTypeMap.isEmpty()) {
            return fieldTypeMap;
        }

        fieldTypeMap.keySet().retainAll(fieldList);

        logger.error("Final - fieldList:{} of fieldTypeMap:{}  ", fieldList, fieldTypeMap);
        return fieldTypeMap;
    }

    public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        logger.debug("Getting fields type:{} - fields:{} ", type, fields);
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
        logger.debug("Final fields:{} of type:{} ", fields, type);
        return fields;
    }

    public static Map<String, String> getFieldTypeMap(Class<?> clazz) {
        logger.debug("clazz:{} ", clazz);
        Map<String, String> propertyTypeMap = new HashMap<>();

        if (clazz == null) {
            return propertyTypeMap;
        }

        List<Field> fields = getAllFields(clazz);
        logger.debug("AllFields:{} ", fields);

        for (Field field : fields) {
            logger.debug(
                    "field:{} , field.getAnnotatedType():{}, field.getAnnotations():{} , field.getType().getAnnotations():{}, field.getType().getCanonicalName():{} , field.getType().getClass():{} , field.getType().getClasses():{} , field.getType().getComponentType():{}",
                    field, field.getAnnotatedType(), field.getAnnotations(), field.getType().getAnnotations(),
                    field.getType().getCanonicalName(), field.getType().getClass(), field.getType().getClasses(),
                    field.getType().getComponentType());
            propertyTypeMap.put(field.getName(), field.getType().getSimpleName());
        }
        logger.debug("Final propertyTypeMap{} ", propertyTypeMap);
        return propertyTypeMap;
    }

    public static Object invokeGetterMethod(Object obj, String variableName) {
        return JsonApplier.getInstance().invokeReflectionGetter(obj, variableName);
    }

    public static boolean isKeyPresentInMap(String key, Map<String, String> map) {
        logger.debug("Check key:{} is present in map:{}", key, map);
        if (StringHelper.isEmpty(key) || map == null || map.isEmpty()) {
            return false;
        }
        logger.debug(" key:{} present in map:{} ?:{}", key, map, map.keySet().contains(key));
        return map.keySet().contains(key);
    }

    public static boolean isAttributeInExclusion(String className, String attribute,
            Map<String, List<String>> exclusionMap) {
        logger.debug("Check if object:{} attribute:{} is in exclusionMap:{}", className, attribute, exclusionMap);
        if (StringHelper.isEmpty(className) || StringHelper.isEmpty(attribute) || exclusionMap == null
                || exclusionMap.isEmpty()) {
            return false;
        }

        logger.debug("Map contains key exclusionMap.keySet().contains(className):{}",
                exclusionMap.keySet().contains(className));

        if (exclusionMap.keySet().contains(className)) {
            if (exclusionMap.get(className) != null) {
                return false;
            } else if (exclusionMap.get(className).contains(attribute)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> getFieldType(Class<?> type, List<FieldFilterData> fieldFilterData) {
        logger.error("After modification type:{}, fieldFilterData:{}", type, fieldFilterData);
        Map<String, String> fieldTypeMap = null;
        if (type == null || fieldFilterData == null || fieldFilterData.isEmpty()) {
            return fieldTypeMap;
        }

        List<String> fieldList = new ArrayList<>();
        for (FieldFilterData entry : fieldFilterData) {
            fieldList.add(entry.getField());
        }
        return getFieldDataType(type, fieldList);
    }

    public static List<Filter> createFilter(Class<?> type, List<FieldFilterData> fieldFilterData, String primaryKey,
            PersistenceEntryManager persistenceEntryManager) {
        logger.error("After modification type:{}, fieldFilterData:{}, primaryKey:{}, persistenceEntryManager:{}", type,
                fieldFilterData, primaryKey, persistenceEntryManager);
        List<Filter> filters = new ArrayList<>();

        if (type == null || fieldFilterData == null || fieldFilterData.isEmpty() || StringUtils.isBlank(primaryKey)
                || persistenceEntryManager == null) {
            return filters;
        }

        Map<String, String> dataTypeMap = getFieldType(type, fieldFilterData);
        logger.error(" type:{} dataTypeMap:{}", type.getCanonicalName(), dataTypeMap);

        for (FieldFilterData entry : fieldFilterData) {

            String dataType = AttributeDataType.STRING.getValue();

            if (dataTypeMap != null && dataTypeMap.containsKey(entry.getField())) {
                dataType = dataTypeMap.get(entry.getField());
            }
            logger.error("entry.getField():{}, dataType:{}", entry.getField(), dataType);

            Filter dataFilter = null;
            if (AttributeDataType.STRING.getValue().equalsIgnoreCase(dataType)) {
                logger.error("entry.getField():{}, dataType:{}, AttributeDataType.STRING.getValue():{}",
                        entry.getField(), dataType, AttributeDataType.STRING.getValue());
                dataFilter = Filter.createEqualityFilter(entry.getField(), entry.getValue());

            } else if (AttributeDataType.DATE.getDisplayName().equalsIgnoreCase(dataType)) {
                logger.error("entry.getField():{}, dataType:{}, AttributeDataType.DATE.getDisplayName():{}",
                        entry.getField(), dataType, AttributeDataType.DATE.getDisplayName());
                Date dateValue = persistenceEntryManager.decodeTime(primaryKey, entry.getValue());
                if (FilterOperator.EQUALITY.getSign().equalsIgnoreCase(entry.getOperator())) {

                    dataFilter = Filter.createEqualityFilter(entry.getField(), dateValue);

                } else if (FilterOperator.GREATER.getSign().equalsIgnoreCase(entry.getOperator())) {

                    dataFilter = Filter.createGreaterOrEqualFilter(entry.getField(), dateValue);

                } else if (FilterOperator.LESS.getSign().equalsIgnoreCase(entry.getOperator())) {

                    dataFilter = Filter.createLessOrEqualFilter(entry.getField(), dateValue);

                }
            }
            logger.error("Filters for  fieldFilterData - dataFilter:{}", dataFilter);
            filters.add(dataFilter);
        }
        return filters;
    }

}
