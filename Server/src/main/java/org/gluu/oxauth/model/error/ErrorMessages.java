/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version January 16, 2019
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

    public List<ErrorMessage> getAuthorize() {
        return authorize;
    }

    public void setAuthorize(List<ErrorMessage> p_authorize) {
        authorize = p_authorize;
    }

    public List<ErrorMessage> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(List<ErrorMessage> p_clientInfo) {
        clientInfo = p_clientInfo;
    }

    public List<ErrorMessage> getEndSession() {
        return endSession;
    }

    public void setEndSession(List<ErrorMessage> p_endSession) {
        endSession = p_endSession;
    }

    public List<ErrorMessage> getRegister() {
        return register;
    }

    public void setRegister(List<ErrorMessage> p_register) {
        register = p_register;
    }

    public List<ErrorMessage> getToken() {
        return token;
    }

    public void setToken(List<ErrorMessage> p_token) {
        token = p_token;
    }

    public List<ErrorMessage> getRevoke() {
        return revoke;
    }

    public void setRevoke(List<ErrorMessage> p_revoke) {
        revoke = p_revoke;
    }

    public List<ErrorMessage> getUma() {
        return uma;
    }

    public void setUma(List<ErrorMessage> p_uma) {
        uma = p_uma;
    }

    public List<ErrorMessage> getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(List<ErrorMessage> p_userInfo) {
        userInfo = p_userInfo;
    }

    public List<ErrorMessage> getFido() {
        return fido;
    }

    public void setFido(List<ErrorMessage> fido) {
        this.fido = fido;
    }

}