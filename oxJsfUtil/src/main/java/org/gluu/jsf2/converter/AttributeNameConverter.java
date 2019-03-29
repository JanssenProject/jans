/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.jsf2.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

import org.gluu.model.GluuAttribute;
import org.gluu.service.AttributeService;

@FacesConverter("org.gluu.jsf2.converter.AttributeNameConverter")
public class AttributeNameConverter implements Converter {

	@Inject
	private AttributeService attributeService;

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		if (value == null) {
			return null;
		}
		return attributeService.getAttributeByName(value);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		String string = null;
		if (value instanceof GluuAttribute) {
			string = ((GluuAttribute) value).getName();
		}

		return string;
	}

}