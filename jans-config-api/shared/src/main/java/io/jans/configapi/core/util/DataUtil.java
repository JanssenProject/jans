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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeFormatter;
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

    public static Map<String, String> getFieldDataType(Class<?> clazz, List<String> fieldList) {
        logger.info("Get FieldDataType - clazz:{}, fieldList:{}", clazz, fieldList);
        Map<String, String> fieldTypeMap = null;
        if (clazz == null || fieldList == null || fieldList.isEmpty()) {
            return fieldTypeMap;
        }

        fieldTypeMap = getFieldTypeMap(clazz);
        if (fieldTypeMap.isEmpty()) {
            return fieldTypeMap;
        }

        fieldTypeMap.keySet().retainAll(fieldList);

        logger.info("Final - fieldList:{} of fieldTypeMap:{}  ", fieldList, fieldTypeMap);
        return fieldTypeMap;
    }

    public static Map<String, String> getFieldType(Class<?> clazz, List<FieldFilterData> fieldFilterData) {
        logger.info("Get field clazz:{}, fieldFilterData:{}", clazz, fieldFilterData);
        Map<String, String> fieldTypeMap = null;
        if (clazz == null || fieldFilterData == null || fieldFilterData.isEmpty()) {
            return fieldTypeMap;
        }

        List<String> fieldList = new ArrayList<>();
        for (FieldFilterData entry : fieldFilterData) {
            fieldList.add(entry.getField());
        }
        return getFieldDataType(clazz, fieldList);
    }

    public static Date formatStrDate(String dateString, String datePattern) {
        logger.debug("Format String Date - dateString:{}: datePattern:{}", dateString, datePattern);
        Date date = null;
        try {
            if (StringUtils.isBlank(dateString)) {
                return date;
            }

            if (StringUtils.isBlank(datePattern)) {
                if (dateString.contains(":")) {
                    datePattern = "yyyy-MM-dd'T'HH:mm:ss";
                } else {
                    datePattern = "yyyy-MM-dd";
                }
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
            logger.debug("Returning datePattern:{}, dateFormat:{} ", datePattern, dateFormat);
            date = dateFormat.parse(dateString);
            logger.debug("Returning dateFormat:{}, date:{} ", dateFormat, date);

        } catch (Exception ex) {
            logger.error("Error while formatting String Date - dateString{" + dateString + "}", ex);
            return date;
        }

        return date;
    }

    public static List<Filter> createFilter(List<FieldFilterData> fieldFilterData, String primaryKey,
            PersistenceEntryManager persistenceEntryManager) {
        logger.info("Create ORM Filter for fieldFilterData:{}, primaryKey:{}, persistenceEntryManager:{}",
                fieldFilterData, primaryKey, persistenceEntryManager);
        List<Filter> filters = new ArrayList<>();

        if (fieldFilterData == null || fieldFilterData.isEmpty()) {
            return filters;
        }

        for (FieldFilterData entry : fieldFilterData) {
            logger.info("FieldFilterData entry:{}", entry);
            String dataType = AttributeDataType.STRING.getValue();

            if (StringUtils.isNotBlank(entry.getType())) {
                dataType = entry.getType();
            }
            logger.info(
                    "entry.getField():{}, dataType:{},  AttributeDataType.STRING.getValue():{}, AttributeDataType.STRING.getValue():{}, AttributeDataType.DATE.getDisplayName():{}",
                    entry.getField(), dataType, AttributeDataType.STRING.getValue(),
                    AttributeDataType.STRING.getValue(), AttributeDataType.DATE.getDisplayName());

            Filter dataFilter = null;
            if (AttributeDataType.STRING.getValue().equalsIgnoreCase(dataType)) {
                dataFilter = Filter.createEqualityFilter(entry.getField(), entry.getValue());
            } else if (AttributeDataType.BOOLEAN.getValue().equalsIgnoreCase(dataType)) {
                dataFilter = Filter.createEqualityFilter(entry.getField(), getBooleanValue(entry.getValue()));
            } else if (AttributeDataType.DATE.getDisplayName().equalsIgnoreCase(dataType)) {
                dataFilter = createDateFilter(entry, primaryKey, persistenceEntryManager);
            } else if ("int".equalsIgnoreCase(dataType) || "integer".equalsIgnoreCase(dataType)) {
                dataFilter = createIntegerFilter(entry);
            } else {
                dataFilter = Filter.createEqualityFilter(entry.getField(), entry.getValue());
            }
            filters.add(dataFilter);
            logger.info("dataFilter:{}", dataFilter);

        }
        logger.info("Final Filters for  fieldFilterData - filters:{}", filters);
        return filters;
    }

    private static boolean getBooleanValue(String strValue) {
        logger.info("Get Boolean Value for strValue:{}", strValue);

        boolean value = false;
        if (StringUtils.isBlank(strValue) || !strValue.toLowerCase().matches("true|false")) {
            return value;
        }

        return Boolean.parseBoolean(strValue);

    }

    private static Filter createDateEqualFilter_1(FieldFilterData fieldFilterData) {
        logger.info("Create Date Filter for fieldFilterData:{}", fieldFilterData);
        Filter dateFilter = null;
        if (fieldFilterData == null) {
            return dateFilter;
        }

        String dateField = fieldFilterData.getField();
        String dateString = fieldFilterData.getValue();
        String dateZ = dateString.endsWith("Z") ? dateString : dateString + "Z";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        logger.error("dateField:{}, dateString:{}, dateZ:{}, formatter:{} ", dateField, dateString, dateZ, formatter);
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateZ, formatter);

            // Format the LocalDateTime object back to an ISO 8601 string
            String isoDateString = dateTime.format(formatter);

            if (dateString.contains(":")) {
                logger.error("DateTime - dateString.contains(:)");

                dateFilter = Filter.createEqualityFilter(dateField, isoDateString);

                logger.error("DateTime -  isoDateString:{}, dateFilter:{} ", isoDateString, dateFilter);
            } else {
//
//                   logger.error("StartOfDay:{}, EndOfDay:{} ", startOfDay, endOfDay);
//                dateFilter = Filter.createANDFilter(Filter.createGreaterOrEqualFilter(dateField, startOfDay),
//                        Filter.createLessOrEqualFilter(dateField, endOfDay));
                dateFilter = Filter.createEqualityFilter(dateField, isoDateString);

                logger.error("Date Filter -  isoDateString:{}, dateFilter:{} ", isoDateString, dateFilter);
            }
        } catch (Exception ex) {
            logger.error(
                    "Error while creating Date filter for {" + dateField + "} with value as {" + dateString + "} is",
                    ex);
            dateFilter = Filter.createEqualityFilter(dateField, dateString);
            return dateFilter;

        }
        return dateFilter;
    }

    private static Filter createDateEqualFilter_2(FieldFilterData fieldFilterData) {
        logger.info("Create Date Filter for fieldFilterData:{}", fieldFilterData);
        Filter dateFilter = null;
        if (fieldFilterData == null) {
            return dateFilter;
        }

        String dateField = fieldFilterData.getField();
        String dateString = fieldFilterData.getValue();
        String dateZ = dateString.endsWith("Z") ? dateString : dateString + "Z";
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        logger.error("dateField:{}, dateString:{}, dateZ:{}, formatter:{} ", dateField, dateString, dateZ, formatter);
        try {

            // Parse the string into an OffsetDateTime object
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateString);
            logger.error("dateString:{}, offsetDateTime:{} ", dateString, offsetDateTime);

            // Format the OffsetDateTime object back to an ISO 8601 string
            String isoString = offsetDateTime.format(formatter);

            dateFilter = Filter.createEqualityFilter(dateField, isoString);
            logger.error("Date Filter -  isoString:{}, dateFilter:{} ", isoString, dateFilter);

        } catch (Exception ex) {
            logger.error(
                    "Error while creating Date filter for {" + dateField + "} with value as {" + dateString + "} is",
                    ex);
            dateFilter = Filter.createEqualityFilter(dateField, dateString);
            return dateFilter;

        }
        return dateFilter;
    }

    private static Filter createDateEqualFilter_3(FieldFilterData fieldFilterData) {
        logger.info("Create Date Filter for fieldFilterData:{}", fieldFilterData);
        Filter dateFilter = null;
        if (fieldFilterData == null) {
            return dateFilter;
        }

        String dateField = fieldFilterData.getField();
        String dateString = fieldFilterData.getValue();
        String dateZ = dateString.endsWith("Z") ? dateString : dateString + "Z";
        DateTimeFormatter sourceFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        logger.error("dateField:{}, dateString:{}, dateZ:{}, sourceFormat:{} ", dateField, dateString, dateZ,
                sourceFormat);
        try {

            LocalDateTime dateTime = LocalDateTime.parse(dateZ, sourceFormat);
            logger.error("dateZ:{}, sourceFormat:{}, dateTime:{} ", dateZ, sourceFormat, dateTime);

            String formatedDateTime = dateTime.atZone(ZoneId.of("UTC")).format(targetFormat);
            logger.error("dateZ:{}, formatedDateTime:{}, targetFormat:{}", dateZ, formatedDateTime, targetFormat);

            dateFilter = Filter.createEqualityFilter(dateField, formatedDateTime);
            logger.error("dateString:{}, dateFilter:{} ", dateString, dateFilter);

        } catch (Exception ex) {
            logger.error(
                    "Error while creating Date filter for {" + dateField + "} with value as {" + dateString + "} is",
                    ex);
            dateFilter = Filter.createEqualityFilter(dateField, dateString);
            return dateFilter;

        }
        return dateFilter;
    }

    private static Filter createDateEqualFilter(FieldFilterData fieldFilterData) {
        logger.info("Create Date Filter for fieldFilterData:{}", fieldFilterData);
        Filter dateFilter = null;
        if (fieldFilterData == null) {
            return dateFilter;
        }

        String dateField = fieldFilterData.getField();
        String dateString = fieldFilterData.getValue();
        String dateZ = dateString.endsWith("Z") ? dateString : dateString + "Z";
        DateTimeFormatter sourceFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateTimeFormatter targetFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        logger.error("dateField:{}, dateString:{}, dateZ:{}, sourceFormat:{} ", dateField, dateString, dateZ,
                sourceFormat);
        try {

            LocalDateTime dateTime = LocalDateTime.parse(dateZ, sourceFormat);
            logger.error("dateZ:{}, sourceFormat:{}, dateTime:{} ", dateZ, sourceFormat, dateTime);

            String formatedDateTime = dateTime.atZone(ZoneId.of("UTC")).format(targetFormat);
            logger.error("dateZ:{}, formatedDateTime:{}, targetFormat:{}", dateZ, formatedDateTime, targetFormat);

            dateFilter = Filter.createEqualityFilter(dateField, formatedDateTime);
            logger.error("dateString:{}, dateFilter:{} ", dateString, dateFilter);

        } catch (Exception ex) {
            logger.error(
                    "Error while creating Date filter for {" + dateField + "} with value as {" + dateString + "} is",
                    ex);
            dateFilter = Filter.createEqualityFilter(dateField, dateString);
            return dateFilter;

        }
        return dateFilter;
    }

    public Date decodeTime(String strDate) {
        logger.error("decodeTime - strDate:{} ", strDate);
        Date date = null;
        if (StringHelper.isEmpty(strDate)) {
            return date;
        }
        try {
            // Add ending Z if necessary
            String dateZ = strDate.endsWith("Z") ? strDate : strDate + "Z";
            logger.error("decodeTime - dateZ:{} ", dateZ);
            date = new Date(Instant.parse(dateZ).toEpochMilli());
            logger.error("decodeTime - date:{} ", date);

        } catch (Exception ex) {

            return date;
        }
        return date;
    }

    private static Filter createDateFilter(FieldFilterData fieldFilterData, String primaryKey,
            PersistenceEntryManager persistenceEntryManager) {
        logger.info("Create Date Filter for fieldFilterData:{}, primaryKey:{}, persistenceEntryManager:{}",
                fieldFilterData, primaryKey, persistenceEntryManager);

        Filter dateFilter = null;
        if (fieldFilterData == null) {
            return dateFilter;
        }

        String strDateField = fieldFilterData.getField();
        String strDateValue = fieldFilterData.getValue();
        String dateZ = strDateValue.endsWith("Z") ? strDateValue : strDateValue + "Z";
        Date dateValue = formatStrDate(dateZ, null);
        logger.info(" strDateField:{}, fieldFilterData.getValue():{}, dateValue:{}", strDateField,
                fieldFilterData.getValue(), dateValue);

        if (FilterOperator.EQUALITY.getSign().equalsIgnoreCase(fieldFilterData.getOperator())) {
            // dateFilter = Filter.createEqualityFilter(strDateField, dateValue);
            dateFilter = createDateEqualFilter(fieldFilterData);
        } else if (FilterOperator.GREATER.getSign().equalsIgnoreCase(fieldFilterData.getOperator())
                || FilterOperator.GREATER_OR_EQUAL.getSign().equalsIgnoreCase(fieldFilterData.getOperator())) {

            dateFilter = Filter.createGreaterOrEqualFilter(strDateField, dateValue);

        } else if (FilterOperator.LESS.getSign().equalsIgnoreCase(fieldFilterData.getOperator())
                || FilterOperator.GREATER_OR_EQUAL.getSign().equalsIgnoreCase(fieldFilterData.getOperator())) {

            dateFilter = Filter.createLessOrEqualFilter(strDateField, dateValue);

        }
        logger.info("Final Date Filter for fieldFilterData:{}, dateFilter:{}", fieldFilterData, dateFilter);
        return dateFilter;
    }

    private static Filter createIntegerFilter(FieldFilterData fieldFilterData) {
        logger.info("Create Integer Filter for fieldFilterData:{}", fieldFilterData);

        Filter dataFilter = null;
        if (fieldFilterData == null) {
            return dataFilter;
        }

        String dataValue = fieldFilterData.getValue();
        Integer intValue = null;
        if (StringUtils.isNotBlank(fieldFilterData.getValue())) {
            try {
                intValue = Integer.parseInt(fieldFilterData.getValue());
            } catch (Exception ex) {
                logger.error("Though Data type is numeric but value is not numeric{" + fieldFilterData.getValue() + "}",
                        ex);
            }

        }
        logger.info("Create Integer Filter for intValue:{}", intValue);

        if (FilterOperator.EQUALITY.getSign().equalsIgnoreCase(fieldFilterData.getOperator())) {

            dataFilter = Filter.createEqualityFilter(fieldFilterData.getField(), dataValue);

        } else if (FilterOperator.GREATER.getSign().equalsIgnoreCase(fieldFilterData.getOperator())
                || FilterOperator.GREATER_OR_EQUAL.getSign().equalsIgnoreCase(fieldFilterData.getOperator())) {

            dataFilter = Filter.createGreaterOrEqualFilter(fieldFilterData.getField(), dataValue);

        } else if (FilterOperator.LESS.getSign().equalsIgnoreCase(fieldFilterData.getOperator())
                || FilterOperator.GREATER_OR_EQUAL.getSign().equalsIgnoreCase(fieldFilterData.getOperator())) {

            dataFilter = Filter.createLessOrEqualFilter(fieldFilterData.getField(), dataValue);

        }
        logger.info("Final Date Filter for fieldFilterData:{}, dataValue:{}", fieldFilterData, dataValue);
        return dataFilter;
    }

}
