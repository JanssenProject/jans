package org.xdi.oxauth.model.error;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 26/12/2012
 */
@XmlRootElement(name = "errors")
@XmlAccessorType(XmlAccessType.FIELD)
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
    @XmlElementWrapper(name = "federation")
    @XmlElement(name = "error")
    private List<ErrorMessage> federation;
    @XmlElementWrapper(name = "register")
    @XmlElement(name = "error")
    private List<ErrorMessage> register;
    @XmlElementWrapper(name = "token")
    @XmlElement(name = "error")
    private List<ErrorMessage> token;
    @XmlElementWrapper(name = "uma")
    @XmlElement(name = "error")
    private List<ErrorMessage> uma;
    @XmlElementWrapper(name = "user-info")
    @XmlElement(name = "error")
    private List<ErrorMessage> userInfo;
    @XmlElementWrapper(name = "validate-token")
    @XmlElement(name = "error")
    private List<ErrorMessage> validateToken;

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

    public List<ErrorMessage> getFederation() {
        return federation;
    }

    public void setFederation(List<ErrorMessage> p_federation) {
        federation = p_federation;
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

    public List<ErrorMessage> getValidateToken() {
        return validateToken;
    }

    public void setValidateToken(List<ErrorMessage> p_validateToken) {
        validateToken = p_validateToken;
    }
}