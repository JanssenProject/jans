/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.validator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;

import io.jans.util.StringHelper;
import io.jans.model.GluuAttribute;
import io.jans.model.attribute.AttributeValidation;

/**
 * Attribute Metadata Validator
 *
 * @author Yuriy Movchan
 *
 * @version 07/28/2017
 */
@FacesValidator("attributeValidator")
public class AttributeValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent comp, Object value) {
        GluuAttribute attribute = (GluuAttribute) comp.getAttributes().get("attribute");

        if (attribute == null) {
            ((UIInput) comp).setValid(true);
            return;
        }

        AttributeValidation attributeValidation = attribute.getAttributeValidation();
        Integer minvalue = attributeValidation != null ? attributeValidation.getMinLength() : null;
        Integer maxValue = attributeValidation != null ? attributeValidation.getMaxLength() : null;
        String regexpValue = attributeValidation != null ? attributeValidation.getRegexp() : null;

        String attributeValue = (String) value;

        // Minimum length validation
        if (minvalue != null) {
            int min = attributeValidation.getMinLength();

            if ((attributeValue != null) && (attributeValue.length() < min)) {
                ((UIInput) comp).setValid(false);

                FacesMessage message = new FacesMessage(attribute.getDisplayName() + " should be at least " + min + " symbols. ");
                message.setSeverity(FacesMessage.SEVERITY_ERROR);
                context.addMessage(comp.getClientId(context), message);
            }
        }

        //default maxlength
        int max = 400;
        if (maxValue != null) {
            max = attributeValidation.getMaxLength();
        }

        // Maximum Length validation
        if ((attributeValue != null) && (attributeValue.length() > max)) {
            ((UIInput) comp).setValid(false);

            FacesMessage message = new FacesMessage(attribute.getDisplayName() + " should not exceed " + max + " symbols. ");
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            context.addMessage(comp.getClientId(context), message);
        }

        // Regex Pattern Validation

        if ((attribute.getName().equalsIgnoreCase("mail") && ((regexpValue == null) || (StringHelper.isEmpty(regexpValue))))) {
            regexpValue = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@"
                    + "[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
        }

        if ((regexpValue != null) && StringHelper.isNotEmpty(regexpValue)) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regexpValue);
            if ((attributeValue != null) && !(attributeValue.trim().equals(""))) {
                java.util.regex.Matcher matcher = pattern.matcher(attributeValue);
                boolean flag = matcher.matches();
                if (!flag) {
                    ((UIInput) comp).setValid(false);

                    FacesMessage message = new FacesMessage(attribute.getDisplayName() + " Format is invalid. ");
                    message.setSeverity(FacesMessage.SEVERITY_ERROR);
                    context.addMessage(comp.getClientId(context), message);
                }
            }
        }

    }

}
