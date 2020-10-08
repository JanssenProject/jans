/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

/**
 * @author Javier Rojas Blum
 * @version August 24, 2016
 */
@FacesValidator("jsonValidator")
public class JSonValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        try {
            if (!Strings.isNullOrEmpty((String) value)) {
                new JSONObject((String) value);
            }
        } catch (JSONException e) {
            FacesMessage msg = new FacesMessage("Invalid JSON format.");
            msg.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(msg);
        }
    }
}