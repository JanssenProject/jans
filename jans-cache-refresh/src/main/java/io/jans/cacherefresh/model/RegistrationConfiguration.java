/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

/**
 * 
 */
package io.jans.cacherefresh.model;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author "Oleksiy Tataryn"
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegistrationConfiguration implements Serializable {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = -7310064771467874959L;

	/**
	 * 
	 */

	private List<String> additionalAttributes;

	private boolean isCaptchaDisabled;

    public List<String> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttributes(List<String> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }

    public boolean isCaptchaDisabled() {
        return isCaptchaDisabled;
    }

    public void setCaptchaDisabled(boolean captchaDisabled) {
        isCaptchaDisabled = captchaDisabled;
    }

}
