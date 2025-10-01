/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.converter;

import org.apache.commons.text.StringEscapeUtils;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

/**
 * @author: Yuriy Movchan Date: 07.11.2012
 */
@FacesConverter("io.jans.jsf2.converter.newLineToBRConverter")
public class NewLineToBRConverter implements Converter {

    public Object getAsObject(FacesContext arg0, UIComponent converter, String str) {
        return str;
    }

    public String getAsString(FacesContext arg0, UIComponent converter, Object obj) {
        return StringEscapeUtils.escapeHtml4((String) obj).replace("\r\n", "<br/>").replace("\n", "<br/>");
    }

}
