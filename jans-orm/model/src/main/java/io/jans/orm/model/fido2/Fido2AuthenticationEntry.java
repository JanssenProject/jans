/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.fido2;

import java.io.Serializable;
import java.util.Date;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;

/**
 * Fido2 registration entry
 *
 * @author Yuriy Movchan
 * @version 11/02/2018
 */
@ObjectClass(value = "jansFido2AuthnEntry")
public class Fido2AuthenticationEntry extends Fido2Entry implements Serializable {

    private static final long serialVersionUID = -2242931562244920584L;

    @JsonObject
    @AttributeName(name = "jansAuthData")
    private Fido2AuthenticationData authenticationData;

    @JsonObject
    @AttributeName(name = "jansStatus")
    private Fido2AuthenticationStatus authenticationStatus;
    
    @AttributeName(name = "jansSessStateId")
    private String sessionStateId;

	@AttributeName(name = "jansApp")
	private String rpId;

    @AttributeName(name = "jansCodeChallengeHash")
    private String challengeHash;

    public Fido2AuthenticationEntry() {
    }

    public Fido2AuthenticationEntry(String dn, String id, Date creationDate, String userInum, Fido2AuthenticationData authenticationData) {
        super(dn, id, creationDate, userInum, authenticationData.getChallenge());
        this.authenticationData = authenticationData;
    }

    public Fido2AuthenticationData getAuthenticationData() {
        return authenticationData;
    }

    public void setAuthenticationData(Fido2AuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
    }

    public Fido2AuthenticationStatus getAuthenticationStatus() {
        return authenticationStatus;
    }

    public void setAuthenticationStatus(Fido2AuthenticationStatus authenticationStatus) {
        this.authenticationStatus = authenticationStatus;
    }

    public String getSessionStateId() {
		return sessionStateId;
	}

	public void setSessionStateId(String sessionStateId) {
		this.sessionStateId = sessionStateId;
	}

	public String getRpId() {
		return rpId;
	}

	public void setRpId(String rpId) {
		this.rpId = rpId;
	}

	public String getChallengeHash() {
		return challengeHash;
	}

	public void setChallengeHash(String challengeHash) {
		this.challengeHash = challengeHash;
	}

	@Override
	public String toString() {
		return "Fido2AuthenticationEntry [authenticationData=" + authenticationData + ", authenticationStatus="
				+ authenticationStatus + ", sessionStateId=" + sessionStateId + ", rpId=" + rpId + ", challengeHash="
				+ challengeHash + "]";
	}
}
