/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.jsf2.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

@FacesConverter(value = "org.gluu.oxtrust.util.jsf.EnumConverter")
public class EnumConverter extends org.jboss.seam.ui.converter.EnumConverter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent comp, String value) throws ConverterException {
		try {
			return super.getAsObject(context, comp, value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
