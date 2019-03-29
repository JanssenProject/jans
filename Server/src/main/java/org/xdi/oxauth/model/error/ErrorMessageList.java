/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.error;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Represents an error message list in a configuration XML file.
 *
 * @author Javier Rojas Date: 09.23.2011
 *
 */
@XmlRootElement(name = "errors")
public class ErrorMessageList {

	@XmlElement(name = "error")
	private ArrayList<ErrorMessage> errors;

	public ArrayList<ErrorMessage> getErrorList() {
		return errors;
	}

	public void setErrorList(ArrayList<ErrorMessage> errorList) {
		this.errors = errorList;
	}
}
