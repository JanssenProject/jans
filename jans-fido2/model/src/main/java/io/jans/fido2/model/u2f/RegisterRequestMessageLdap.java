/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f;

import io.jans.fido2.model.u2f.protocol.RegisterRequestMessage;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.JsonObject;

import java.io.Serializable;
import java.util.Date;

/**
 * U2F registration requests
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public class RegisterRequestMessageLdap extends RequestMessageLdap implements Serializable {

    private static final long serialVersionUID = -2242931562244920584L;

    @JsonObject
    @AttributeName(name = "jansReq")
    private RegisterRequestMessage registerRequestMessage;

    public RegisterRequestMessageLdap() {
    }

    public RegisterRequestMessageLdap(RegisterRequestMessage registerRequestMessage) {
        this.registerRequestMessage = registerRequestMessage;
        this.requestId = registerRequestMessage.getRequestId();
    }

    public RegisterRequestMessageLdap(String dn, String id, Date creationDate, String sessionId, String userInum,
                                      RegisterRequestMessage registerRequestMessage) {
        super(dn, id, registerRequestMessage.getRequestId(), creationDate, sessionId, userInum);
        this.registerRequestMessage = registerRequestMessage;
    }

    public RegisterRequestMessage getRegisterRequestMessage() {
        return registerRequestMessage;
    }

    public void setRegisterRequestMessage(RegisterRequestMessage registerRequestMessage) {
        this.registerRequestMessage = registerRequestMessage;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RegisterRequestMessageLdap [id=").append(id).append(", registerRequestMessage=").append(registerRequestMessage).append(", requestId=")
                .append(requestId).append(", creationDate=").append(creationDate).append("]");
        return builder.toString();
    }

}
