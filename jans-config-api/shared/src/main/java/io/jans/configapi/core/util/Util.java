/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

import io.jans.configapi.core.service.ConfService;
import io.jans.model.SearchRequest;
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

import org.apache.commons.lang.StringUtils;
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
            StringTokenizer st = new StringTokenizer(str, fieldValueSeparator);

            if (StringUtils.isNotBlank(data) && st.hasMoreTokens()) {
                String[] keyValue = data.split("=");
                log.debug("fieldValueMap:{},keyValue:{}, keyValue[0]:{}, keyValue[1]):{}", fieldValueMap, keyValue,
                        keyValue[0], keyValue[1]);
                fieldValueMap.put(keyValue[0], keyValue[1]);
            }
        }

        log.info("fieldValueMap:{}", fieldValueMap);

        // Replace filedValue with the DB field name
        fieldValueMap = getAttributeData(entityClass, fieldValueMap);
        return fieldValueMap;
    }

    public Map<String, String> getAttributeData(Class<?> entityClass, Map<String, String> fieldValueMap) {
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
        if (fieldValueMap != null && !fieldValueMap.isEmpty()) {

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
}
