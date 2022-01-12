/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util;

import java.util.Iterator;
import java.util.Properties;


/**
 * @author Yuriy Movchan Date: 05/11/2019
 */
public final class PropertiesHelper {

    public static Properties filterProperties(Properties conf, String prefix) {
    	String findKey = prefix + ".";
    	int findKeyLength = findKey.length();

    	Properties resultConf = new Properties();

    	Iterator<?> keys = conf.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            
            if (key.startsWith(findKey)) {
                String resultKey = key.substring(findKeyLength);
                String value = (String) conf.getProperty(key);

                resultConf.put(resultKey, value);
            }
        }
        
        return resultConf;
    }

    public static Properties findProperties(Properties conf, String prefix) {
    	String findKey = prefix + ".";

    	Properties resultConf = new Properties();

    	Iterator<?> keys = conf.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            
            if (key.startsWith(findKey)) {
                String value = (String) conf.getProperty(key);

                resultConf.put(key, value);
            }
        }
        
        return resultConf;
    }

    public static Properties appendPrefix(Properties conf, String prefix) {
    	String appendToKey = prefix + ".";

    	Properties resultConf = new Properties();

    	Iterator<?> keys = conf.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            
            resultConf.put(appendToKey + key, conf.getProperty(key));
        }
        
        return resultConf;
    }
}
