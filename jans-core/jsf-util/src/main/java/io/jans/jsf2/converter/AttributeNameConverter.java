/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

import io.jans.model.GluuAttribute;
import io.jans.service.AttributeService;

@FacesConverter("io.jans.jsf2.converter.AttributeNameConverter")
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