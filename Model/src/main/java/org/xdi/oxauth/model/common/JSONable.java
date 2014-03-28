package org.xdi.oxauth.model.common;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * @author Javier Rojas Blum Date: 13.01.2013
 */
public interface JSONable {

    JSONObject toJSONObject() throws JSONException;
}