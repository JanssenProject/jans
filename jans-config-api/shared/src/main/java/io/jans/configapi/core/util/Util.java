/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

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

    @Inject
    Logger log;

    public static String escapeLog(Object param) {
        if (param == null)
            return "";
        return param.toString().replaceAll("[\n\r\t]", "_");
    }

    public List<String> getTokens(String str, String tokenizer) {
        log.error(" String to get tokens - str:{}, tokenizer:{}", str, tokenizer);

        ArrayList<String> list = new ArrayList<>();
        if (StringUtils.isBlank(str)) {
            list.add("");
            return list;
        }

        log.error("str.contains(tokenizer):{}", str.contains(tokenizer));
        if (!str.contains(tokenizer)) {

            list.add(str);
            log.error(" Not tokenized - list:{}", list);
            return list;
        }

        log.error("final tokenized list:{}", Collections.list(new StringTokenizer(str, tokenizer)).stream()
                .map(token -> (String) token).collect(Collectors.toList()));
        return Collections.list(new StringTokenizer(str, tokenizer)).stream().map(token -> (String) token)
                .collect(Collectors.toList());
    }

    public Map<String, String> getFieldValueMap(String str, String tokenizer, String fieldValueSeparator) {
        log.error(" Field Value to get map - str:{}, tokenizer:{} fieldValueSeparator:{}", str, tokenizer,
                fieldValueSeparator);

        Map<String, String> fieldValueMap = new HashMap<>();
        log.error(" StringUtils.isBlank(str):{}, str.contains(tokenizer):{} , str.contains(fieldValueSeparator):{}", StringUtils.isBlank(str), str.contains(tokenizer),
                str.contains(fieldValueSeparator));
        if (StringUtils.isBlank(str) || !str.contains(fieldValueSeparator)) {
            return fieldValueMap;
        }

        log.error("getTokens(str, tokenizer):{}", getTokens(str, tokenizer));

        List<String> fieldValueList = getTokens(str, tokenizer);
        log.error("fieldValueList:{}", fieldValueList);
        if (fieldValueList == null || fieldValueList.isEmpty()) {
            return fieldValueMap;
        }

        for (String data : fieldValueList) {
            StringTokenizer st = new StringTokenizer(str, fieldValueSeparator);

            if (StringUtils.isNotBlank(data) && st.hasMoreTokens()) {
                String[] keyValue = data.split("=");
                log.error("fieldValueMap:{},keyValue:{}, keyValue[0]:{}, keyValue[1]):{}", fieldValueMap, keyValue, keyValue[0], keyValue[1]);
                fieldValueMap.put(keyValue[0], keyValue[1]);
            }
        }

        log.error("fieldValueMap:{}", fieldValueMap);
        return fieldValueMap;
    }
}
