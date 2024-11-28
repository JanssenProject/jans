/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

import io.jans.configapi.core.service.ConfService;
import io.jans.model.FieldFilterData;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.AttributesList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class Util {

    private static final Class<?>[] LDAP_ENTRY_PROPERTY_ANNOTATIONS = { AttributeName.class, AttributesList.class };

    @Inject
    Logger log;

    @Inject
    ConfService confService;

    public static String escapeLog(Object param) {
        if (param == null)
            return "";
        return param.toString().replaceAll("[\n\r\t]", "_");
    }

    public List<String> getTokens(String str, String tokenizer) {
        if (log.isInfoEnabled()) {
            log.info(" String to get tokens - str:{}, tokenizer:{}", escapeLog(str), escapeLog(tokenizer));
        }

        ArrayList<String> list = new ArrayList<>();
        if (StringUtils.isBlank(str)) {
            list.add("");
            return list;
        }

        log.trace("str.contains(tokenizer):{}", str.contains(tokenizer));
        if (!str.contains(tokenizer)) {

            list.add(str);
            log.trace(" Not tokenized string - list:{}", list);
            return list;
        }

        log.info("final tokenized list:{}", Collections.list(new StringTokenizer(str, tokenizer)).stream()
                .map(token -> (String) token).collect(Collectors.toList()));

        return Collections.list(new StringTokenizer(str, tokenizer)).stream().map(token -> (String) token)
                .collect(Collectors.toList());
    }

    public Map<String, String> getFieldValueMap(Class<?> entityClass, String str, String tokenizer,
            String fieldValueSeparator) {
        if (log.isInfoEnabled()) {
            log.info(" Field Value to get map - entityClass:{}, str:{}, tokenizer:{} fieldValueSeparator:{}",
                    escapeLog(entityClass), escapeLog(str), escapeLog(tokenizer), escapeLog(fieldValueSeparator));
        }

        Map<String, String> fieldValueMap = new HashMap<>();

        if (StringUtils.isBlank(str) || !str.contains(fieldValueSeparator)) {
            return fieldValueMap;
        }

        log.trace("getTokens(str, tokenizer):{}", getTokens(str, tokenizer));

        List<String> fieldValueList = getTokens(str, tokenizer);
        log.debug("fieldValueList:{}", fieldValueList);

        if (fieldValueList == null || fieldValueList.isEmpty()) {
            return fieldValueMap;
        }

        for (String data : fieldValueList) {
            String[] result = data.split(fieldValueSeparator);

            log.debug("fieldValueSeparator:{}, result:{}", fieldValueSeparator, result);
            if (result != null && result.length > 0) {
                String fieldName = result[0];
                String value = null;
                if (result.length > 1) {
                    value = result[1];
                }
                log.info("fieldName:{},value:{}", fieldName, value);
                fieldValueMap.put(fieldName, value);
            }
        }

        log.info("fieldValueMap:{}", fieldValueMap);

        // Replace filedValue with the DB field name
        fieldValueMap = getAttributeData(entityClass, fieldValueMap);
        return fieldValueMap;
    }

    public List<FieldFilterData> getFieldValueList(Class<?> entityClass, String str, String tokenizer,
            List<String> fieldValueSeparator) {
        if (log.isInfoEnabled()) {
            log.info(" Get FieldValueList - entityClass:{}, str:{}, tokenizer:{} fieldValueSeparator:{}",
                    escapeLog(entityClass), escapeLog(str), escapeLog(tokenizer), escapeLog(fieldValueSeparator));
        }

        List<FieldFilterData> fieldFilterDataList = new ArrayList<>();

        if (StringUtils.isBlank(str)) {
            return fieldFilterDataList;
        }

        List<String> fieldValueList = getTokens(str, tokenizer);
        log.debug("After tokenizing fieldValueList:{}", fieldValueList);

        if (fieldValueList == null || fieldValueList.isEmpty()) {
            return fieldFilterDataList;
        }

        Map<String, List<Annotation>> propertiesAnnotations = getPropertiesAnnotations(entityClass);
        Map<String, String> fieldTypeMap = DataUtil.getFieldTypeMap(entityClass);

        for (String data : fieldValueList) {
            if (StringUtils.isNotBlank(data)) {
                FieldFilterData fieldFilterData = this.getFieldFilterData(propertiesAnnotations, fieldTypeMap, data,
                        fieldValueSeparator);
                fieldFilterDataList.add(fieldFilterData);
            }
        }

        log.info("Returning fieldFilterDataList:{}", fieldFilterDataList);

        return fieldFilterDataList;
    }

    private FieldFilterData getFieldFilterData(Map<String, List<Annotation>> propertiesAnnotations,
            Map<String, String> fieldTypeMap, String dataStr, List<String> fieldValueSeparator) {
        log.info("Get FieldFilterData - dataStr:{} , fieldValueSeparator:{}", dataStr, fieldValueSeparator);

        FieldFilterData fieldFilterData = null;
        if (StringUtils.isBlank(dataStr) || fieldValueSeparator == null || fieldValueSeparator.isEmpty()) {
            return fieldFilterData;
        }
        for (String separator : fieldValueSeparator) {
            if (dataStr.contains(separator)) {
                String[] result = dataStr.split(separator);

                log.debug("separator:{}, result:{}", separator, result);
                if (result != null && result.length > 0) {
                    String fieldName = result[0];
                    String value = null;
                    if (result.length > 1) {
                        value = result[1];
                    }
                    String dbFieldName = getFieldDBName(fieldName, propertiesAnnotations.get(fieldName));
                    String fieldType = getFieldDataType(fieldName, fieldTypeMap);
                    log.debug("fieldName:{}, dbFieldName:{}, fieldType:{}, value:{}, separator:{}", fieldName,
                            dbFieldName, fieldType, value, separator);
                    fieldFilterData = new FieldFilterData(dbFieldName, separator, value, fieldType);

                }
            }
            log.info("Final fieldFilterData:{}", fieldFilterData);
        }
        return fieldFilterData;

    }

    private String getFieldDataType(String fieldName, Map<String, String> fieldTypeMap) {
        log.info("Get data type for fieldName:{}, fieldTypeMap:{}", fieldName, fieldTypeMap);
        String fieldType = "String";
        if (StringUtils.isBlank(fieldName) || fieldTypeMap == null || fieldTypeMap.isEmpty()) {
            return fieldType;
        }

        return fieldTypeMap.get(fieldName);
    }

    private Map<String, String> getAttributeData(Class<?> entityClass, Map<String, String> fieldValueMap) {
        if (log.isInfoEnabled()) {
            log.info("AttributeData details to be fetched for entityClass:{} with fieldValueMap:{} ",
                    escapeLog(entityClass), escapeLog(fieldValueMap));
        }

        if (entityClass == null || fieldValueMap == null || fieldValueMap.isEmpty()) {
            return fieldValueMap;
        }

        Map<String, List<Annotation>> propertiesAnnotations = confService.getPropertiesAnnotations(entityClass,
                LDAP_ENTRY_PROPERTY_ANNOTATIONS);
        log.debug("Properties annotations fetched for theClass:{} are propertiesAnnotations:{}", entityClass,
                propertiesAnnotations);

        if (propertiesAnnotations == null || propertiesAnnotations.isEmpty()) {
            return fieldValueMap;
        }

        Map<String, String> updatedFieldValueMap = new HashMap<>();
        if (!fieldValueMap.isEmpty()) {

            for (Map.Entry<String, String> entry : fieldValueMap.entrySet()) {
                log.debug("entry.getKey():{}, entry.getValue():{}", entry.getKey(), entry.getValue());
                String dbFieldName = getFieldDBName(entry.getKey(), propertiesAnnotations.get(entry.getKey()));
                if (StringUtils.isNotBlank(dbFieldName)) {
                    updatedFieldValueMap.put(dbFieldName, entry.getValue());
                } else {
                    updatedFieldValueMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        log.info("Returning updatedFieldValueMap:{} ", updatedFieldValueMap);
        return updatedFieldValueMap;
    }

    private String getFieldDBName(String fieldName, List<Annotation> annotations) {
        log.info("DB field to be fetched for fieldName:{} are annotations:{}", fieldName, annotations);
        if (StringUtils.isBlank(fieldName) || (annotations == null || annotations.isEmpty())) {
            return fieldName;
        }

        for (Annotation annotation : annotations) {
            try {
                AttributeName attributeName = (AttributeName) annotation;
                if (attributeName != null && StringUtils.isNotBlank(attributeName.name())) {
                    fieldName = attributeName.name();
                }
            } catch (Exception ex) {
                log.error("Error while fetching DB fieldName for fieldName:{} is :{}", fieldName, ex);
            }
        }

        log.info("Final DB field fieldName:{} ", fieldName);
        return fieldName;
    }

    private Map<String, List<Annotation>> getPropertiesAnnotations(Class<?> entityClass) {
        log.info("Get propertiesAnnotations for entityClass:{}", entityClass);

        Map<String, List<Annotation>> propertiesAnnotations = null;
        if (entityClass == null) {
            return propertiesAnnotations;
        }

        propertiesAnnotations = confService.getPropertiesAnnotations(entityClass, LDAP_ENTRY_PROPERTY_ANNOTATIONS);
        log.debug("Properties annotations fetched for theClass:{} are propertiesAnnotations:{}", entityClass,
                propertiesAnnotations);

        return propertiesAnnotations;

    }
}
