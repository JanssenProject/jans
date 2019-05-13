/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

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

}
