/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;


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
