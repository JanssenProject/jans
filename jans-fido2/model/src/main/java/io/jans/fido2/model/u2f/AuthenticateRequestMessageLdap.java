/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f;

import io.jans.fido2.model.u2f.protocol.AuthenticateRequestMessage;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.JsonObject;

import java.io.Serializable;
import java.util.Date;

/**
 * U2F authentication requests
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
public class AuthenticateRequestMessageLdap extends RequestMessageLdap implements Serializable {

    private static final long serialVersionUID = -1142931562244920584L;

    @JsonObject
    @AttributeName(name = "jansReq")
    private AuthenticateRequestMessage authenticateRequestMessage;

    public AuthenticateRequestMessageLdap() {
    }

    public AuthenticateRequestMessageLdap(AuthenticateRequestMessage authenticateRequestMessage) {
        this.authenticateRequestMessage = authenticateRequestMessage;
        this.requestId = authenticateRequestMessage.getRequestId();
    }

    public AuthenticateRequestMessageLdap(String dn, String id, Date creationDate, String sessionId, String userInum,
                                          AuthenticateRequestMessage authenticateRequestMessage) {
        super(dn, id, authenticateRequestMessage.getRequestId(), creationDate, sessionId, userInum);
        this.authenticateRequestMessage = authenticateRequestMessage;
    }

    public AuthenticateRequestMessage getAuthenticateRequestMessage() {
        return authenticateRequestMessage;
    }

    public void setAuthenticateRequestMessage(AuthenticateRequestMessage authenticateRequestMessage) {
        this.authenticateRequestMessage = authenticateRequestMessage;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuthenticateRequestMessageLdap [id=").append(id).append(", authenticateRequestMessage=").append(authenticateRequestMessage)
                .append(", requestId=").append(requestId).append(", creationDate=").append(creationDate).append("]");
        return builder.toString();
    }

}
