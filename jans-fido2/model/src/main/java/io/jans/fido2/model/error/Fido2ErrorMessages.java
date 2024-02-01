/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;
import io.jans.model.error.ErrorMessage;
import jakarta.xml.bind.annotation.*;

import java.util.List;

@XmlRootElement(name = "errors")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2ErrorMessages implements Configuration {

    @XmlElementWrapper(name = "common")
    @XmlElement(name = "error")
    private List<ErrorMessage> common;

    @XmlElementWrapper(name = "assertion")
    @XmlElement(name = "error")
    private List<ErrorMessage> assertion;

    @XmlElementWrapper(name = "attestation")
    @XmlElement(name = "error")
    private List<ErrorMessage> attestation;

    public List<ErrorMessage> getCommon() {
        return common;
    }

    public void setCommon(List<ErrorMessage> common) {
        this.common = common;
    }

    public List<ErrorMessage> getAssertion() {
        return assertion;
    }

    public void setAssertion(List<ErrorMessage> assertion) {
        this.assertion = assertion;
    }

    public List<ErrorMessage> getAttestation() {
        return attestation;
    }

    public void setAttestation(List<ErrorMessage> attestation) {
        this.attestation = attestation;
    }
}
