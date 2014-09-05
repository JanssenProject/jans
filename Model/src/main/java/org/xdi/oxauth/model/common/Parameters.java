/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/12/2012
 */

public enum Parameters implements HasParamName {
    SESSION_ID("session_id"),
    REQUEST_SESSION_ID("request_session_id");

    private String paramName;
    private String nameToAppend;

    private Parameters(String p_paramName) {
        paramName = p_paramName;
        nameToAppend = "&" + p_paramName + "=";
    }

    public String getParamName() {
        return paramName;
    }

    public String nameToAppend() {
        return nameToAppend;
    }
}
