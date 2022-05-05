/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * <p>
 * Represents an error message in a configuration XML file.
 * </p>
 * <p>
 * The attribute id is REQUIRED. A single error code.
 * </p>
 * <p>
 * The element description is OPTIONAL. A human-readable UTF-8 encoded text
 * providing additional information, used to assist the client developer in
 * understanding the error that occurred.
 * </p>
 * <p>
 * The element URI is OPTIONAL. A URI identifying a human-readable web page with
 * information about the error, used to provide the client developer with
 * additional information about the error.
 * </p>
 *
 * @author Javier Rojas Date: 09.23.2011
 */
@XmlRootElement(name = "error")
public class ErrorMessage {

    private String id;
    private String description;
    private String uri;

    public ErrorMessage() {
    }

    public ErrorMessage(String id, String description, String uri) {
        this.id = id;
        this.description = description;
        this.uri = uri;
    }

    @XmlAttribute(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlElement(name = "error-description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(name = "error-uri")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
