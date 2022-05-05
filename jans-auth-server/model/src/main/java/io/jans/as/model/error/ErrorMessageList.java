/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;


/**
 * Represents an error message list in a configuration XML file.
 *
 * @author Javier Rojas Date: 09.23.2011
 */
@XmlRootElement(name = "errors")
public class ErrorMessageList {

    @XmlElement(name = "error")
    private List<ErrorMessage> errors;

    public List<ErrorMessage> getErrorList() {
        return errors;
    }

    public void setErrorList(List<ErrorMessage> errorList) {
        this.errors = errorList;
    }
}
