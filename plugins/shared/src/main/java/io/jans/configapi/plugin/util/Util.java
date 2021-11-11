/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    public static String escapeLog(Object param) {
        if (param == null)
            return "";
        return param.toString().replaceAll("[\n\r\t]", "_");
    }
  
}
