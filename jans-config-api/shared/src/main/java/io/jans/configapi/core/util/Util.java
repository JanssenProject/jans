/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.util;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Util {
    
    private Util() { }
    
    public static String escapeLog(Object param) {
        if (param == null)
            return "";
        return param.toString().replaceAll("[\n\r\t]", "_");
    }

}
