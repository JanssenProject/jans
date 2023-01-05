/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.util;

import io.jans.as.server.i18n.LanguageBean;
import io.jans.model.attribute.AttributeValidation;
import io.jans.service.AttributeService;
import io.jans.service.cdi.util.CdiUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.ValidatorException;
import jakarta.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@FacesValidator(value = "gluuPasswordValidator", managed = true)
public class PasswordValidator implements jakarta.faces.validator.Validator {

    private static final String USER_PASSWORD = "userPassword";
    private String newPassword;
    private Pattern pattern;
    private Matcher matcher;
    private boolean hasValidation = false;

    @Inject
    private AttributeService attributeService;

    @Inject
    private LanguageBean languageBean;

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        // This is workaround unless this bug will be fixed in JSF
        if (attributeService == null) {
            attributeService = CdiUtil.bean(AttributeService.class);
        }
        if (languageBean == null) {
            languageBean = CdiUtil.bean(LanguageBean.class);
        }

        AttributeValidation validation = attributeService.getAttributeByName(USER_PASSWORD).getAttributeValidation();
        if (validation != null) {
            String regexp = validation.getRegexp();
            if (regexp != null && !regexp.isEmpty()) {
                pattern = Pattern.compile(regexp);
                matcher = pattern.matcher(value.toString());
                hasValidation = true;
            }
        }
        if (hasValidation && !matcher.matches()) {
            String message = languageBean.getMessage("password.validation.invalid");
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message));
            context.validationFailed();
            ((UIInput) component).setValid(false);
        }

    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
