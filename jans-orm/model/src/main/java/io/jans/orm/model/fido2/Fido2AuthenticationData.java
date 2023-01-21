/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model.fido2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2AuthenticationData extends Fido2Data {

    private static final long serialVersionUID = 1382804326976802044L;

    private String id;
    private String username;
    private String domain;
    private String userId;
    private String challenge;

    private String assertionRequest;
    private String assertionResponse;

    private UserVerification userVerificationOption;

    private Fido2AuthenticationStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getAssertionRequest() {
		return assertionRequest;
	}

	public void setAssertionRequest(String assertionRequest) {
		this.assertionRequest = assertionRequest;
	}

	public String getAssertionResponse() {
        return assertionResponse;
    }

    public void setAssertionResponse(String assertionResponse) {
        this.assertionResponse = assertionResponse;
    }

    public UserVerification getUserVerificationOption() {
        return userVerificationOption;
    }

    public void setUserVerificationOption(UserVerification userVerificationOption) {
        this.userVerificationOption = userVerificationOption;
    }

    public Fido2AuthenticationStatus getStatus() {
        return status;
    }

    public void setStatus(Fido2AuthenticationStatus status) {
        this.status = status;
    }

	@Override
	public String toString() {
		return "Fido2AuthenticationData [id=" + id + ", username=" + username + ", domain=" + domain
				+ ", userId=" + userId + ", challenge=" + challenge + ", assertionRequest=" + assertionRequest + ", assertionResponse="
				+ assertionResponse + ", userVerificationOption=" + userVerificationOption + ", status=" + status + "]";
	}

}
