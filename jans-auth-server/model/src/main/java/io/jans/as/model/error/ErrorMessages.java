/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@XmlRootElement(name = "errors")
@XmlAccessorType(XmlAccessType.FIELD)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorMessages {

    @XmlElementWrapper(name = "authorize")
    @XmlElement(name = "error")
    private List<ErrorMessage> authorize;
    @XmlElementWrapper(name = "client-info")
    @XmlElement(name = "error")
    private List<ErrorMessage> clientInfo;
    @XmlElementWrapper(name = "end-session")
    @XmlElement(name = "error")
    private List<ErrorMessage> endSession;
    @XmlElementWrapper(name = "register")
    @XmlElement(name = "error")
    private List<ErrorMessage> register;
    @XmlElementWrapper(name = "token")
    @XmlElement(name = "error")
    private List<ErrorMessage> token;
    @XmlElementWrapper(name = "revoke")
    @XmlElement(name = "error")
    private List<ErrorMessage> revoke;
    @XmlElementWrapper(name = "uma")
    @XmlElement(name = "error")
    private List<ErrorMessage> uma;
    @XmlElementWrapper(name = "user-info")
    @XmlElement(name = "error")
    private List<ErrorMessage> userInfo;

    @XmlElementWrapper(name = "fido")
    @XmlElement(name = "error")
    private List<ErrorMessage> fido;

    @XmlElementWrapper(name = "backchannelAuthentication")
    @XmlElement(name = "error")
    private List<ErrorMessage> backchannelAuthentication;

    @XmlElementWrapper(name = "ssa")
    @XmlElement(name = "error")
    private List<ErrorMessage> ssa;

    public List<ErrorMessage> getAuthorize() {
        return authorize;
    }

    public void setAuthorize(List<ErrorMessage> authorize) {
        this.authorize = authorize;
    }

    public List<ErrorMessage> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(List<ErrorMessage> clientInfo) {
        this.clientInfo = clientInfo;
    }

    public List<ErrorMessage> getEndSession() {
        return endSession;
    }

    public void setEndSession(List<ErrorMessage> endSession) {
        this.endSession = endSession;
    }

    public List<ErrorMessage> getRegister() {
        return register;
    }

    public void setRegister(List<ErrorMessage> register) {
        this.register = register;
    }

    public List<ErrorMessage> getToken() {
        return token;
    }

    public void setToken(List<ErrorMessage> token) {
        this.token = token;
    }

    public List<ErrorMessage> getRevoke() {
        return revoke;
    }

    public void setRevoke(List<ErrorMessage> revoke) {
        this.revoke = revoke;
    }

    public List<ErrorMessage> getUma() {
        return uma;
    }

    public void setUma(List<ErrorMessage> uma) {
        this.uma = uma;
    }

    public List<ErrorMessage> getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(List<ErrorMessage> userInfo) {
        this.userInfo = userInfo;
    }

    public List<ErrorMessage> getFido() {
        return fido;
    }

    public void setFido(List<ErrorMessage> fido) {
        this.fido = fido;
    }

    public List<ErrorMessage> getBackchannelAuthentication() {
        return backchannelAuthentication;
    }

    public void setBackchannelAuthentication(List<ErrorMessage> backchannelAuthentication) {
        this.backchannelAuthentication = backchannelAuthentication;
    }

    public List<ErrorMessage> getSsa() {
        return ssa;
    }

    public void setSsa(List<ErrorMessage> ssa) {
        this.ssa = ssa;
    }
}