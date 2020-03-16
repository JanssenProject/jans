/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2019, Gluu
 */
package org.gluu.oxauth.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 26/12/2012
 */

public class ClientUtil {

    private final static Logger log = LoggerFactory.getLogger(ClientUtil.class);

    public static String toPrettyJson(JSONObject jsonObject) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonOrgModule());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
    }

    public static List<String> extractListByKey(JSONObject jsonObject, String key) {
        final List<String> result = new ArrayList<String>();
        if (jsonObject.has(key)) {
            JSONArray arrayOfValues = jsonObject.optJSONArray(key);
            if (arrayOfValues != null) {
                for (int i = 0; i < arrayOfValues.length(); i++) {
                    result.add(arrayOfValues.getString(i));
                }
                return result;
            }
            String listString = jsonObject.optString(key);
            if (StringUtils.isNotBlank(listString)) {
                String[] arrayOfStringValues = listString.split(" ");
                for (String c : arrayOfStringValues) {
                    if (StringUtils.isNotBlank(c)) {
                        result.add(c);
                    }
                }
            }
        }
        return result;
    }

}
