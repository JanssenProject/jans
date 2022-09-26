/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    
    public List<String> getTokens(String str, String format) {
        log.error(" String to get tokens - str:{}, format:{}", str, format);
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        
        log.error("str.contains(format):{}", str.contains(format));
        if( !str.contains(format)) {
            ArrayList<String> list = new ArrayList();
            list.add(str);
            log.debug(" Not tokenized - list:{}", list);
            return (list);
        }
        
        log.error("final tokenized list:{}", Collections.list(new StringTokenizer(str, format)).stream().map(token -> (String) token)
                .collect(Collectors.toList()));
        return Collections.list(new StringTokenizer(str, format)).stream().map(token -> (String) token)
                .collect(Collectors.toList());
    }
}
