/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2020 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.fido2.model.entry;

import org.gluu.fido2.ctap.UserVerification;

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
