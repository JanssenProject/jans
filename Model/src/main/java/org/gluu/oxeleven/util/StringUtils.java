package org.gluu.oxeleven.util;

import org.codehaus.jettison.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version October 5, 2016
 */
public class StringUtils {

    public static List<String> toList(JSONArray jsonArray) {
        List<String> list = new ArrayList<String>();

        try {
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(jsonArray.getString(i));
                }
            }
        } catch (Exception ex) {
        }

        return list;
    }
}
