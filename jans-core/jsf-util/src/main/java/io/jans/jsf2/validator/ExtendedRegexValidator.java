/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.validator;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.RegexValidator;

@FacesValidator("extendedRegexValidator")
public class ExtendedRegexValidator extends RegexValidator {

    public ExtendedRegexValidator() {
        String pattern = ".*";

        setPattern(pattern);
    }

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) {
        String pattern = (String) component.getAttributes().get("pattern");

        if (pattern != null) {
            setPattern(pattern);
            super.validate(context, component, value);
        }
    }

}
