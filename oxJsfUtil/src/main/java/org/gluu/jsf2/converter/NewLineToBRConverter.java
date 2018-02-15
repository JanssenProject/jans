/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.jsf2.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * @author: Yuriy Movchan Date: 07.11.2012
 */
@FacesConverter("org.gluu.jsf2.converter.newLineToBRConverter")
public class NewLineToBRConverter implements Converter {

    public Object getAsObject(FacesContext arg0, UIComponent converter, String str) {
        return str;
    }

    public String getAsString(FacesContext arg0, UIComponent converter, Object obj) {
        return StringEscapeUtils.escapeHtml((String) obj).replace("\r\n", "<br/>").replace("\n", "<br/>");
    }

}
