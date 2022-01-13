/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * An ASCII string value that specifies how the Authorization Server displays
 * the authentication and consent user interface pages to the End-User.
 *
 * @author Javier Rojas Blum Date: 02.10.2012
 */
public enum Display implements HasParamName {

    /**
     * The Authorization Server SHOULD display authentication and consent UI
     * consistent with a full user-agent page view. If the display parameter
     * is not specified this is the default display mode.
     */
    PAGE("page"),
    /**
     * The Authorization Server SHOULD display authentication and consent UI
     * consistent with a popup user-agent window. The popup user-agent window
     * SHOULD be 450 pixels wide and 500 pixels tall.
     */
    POPUP("popup"),
    /**
     * The Authorization Server SHOULD display authentication and consent UI
     * consistent with a device that leverages a touch interface.
     * The Authorization Server MAY attempt to detect the touch device and
     * further customize the interface.
     */
    TOUCH("touch"),
    /**
     * The Authorization Server SHOULD display authentication and consent UI
     * consistent with a "feature phone" type display.
     */
    WAP("wap"),
    /**
     * The Authorization Server SHOULD display authentication and consent UI
     * consistent with the limitations of an embedded user-agent.
     */
    EMBEDDED("embedded");

    private final String paramName;

    Display(String paramName) {
        this.paramName = paramName;
    }

    public String getParamName() {
        return paramName;
    }

    /**
     * Returns the corresponding {@link Display} for a parameter
     * display of the authorization endpoint.
     *
     * @param param The parameter.
     * @return The corresponding response type if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static Display fromString(String param) {
        if (param != null) {
            for (Display rt : Display.values()) {
                if (param.equals(rt.paramName)) {
                    return rt;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     */
    @Override
    @JsonValue
    public String toString() {
        return paramName;
    }
}