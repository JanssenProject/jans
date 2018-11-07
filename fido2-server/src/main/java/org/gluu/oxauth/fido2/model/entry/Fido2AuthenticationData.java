/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.model.entry;

public class Fido2AuthenticationData extends Fido2Data {

    private static final long serialVersionUID = 1382804326976802044L;

    private String id;

    private String registrationId;

    private String username;

    private String domain;

    private String userId;

    private String challenge;

    private String w3cCredentialRequestOptions;

    private String w3cAuthenticatorAssertionResponse;

    private String userVerificationOption;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
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

    public String getW3cCredentialRequestOptions() {
        return w3cCredentialRequestOptions;
    }

    public void setW3cCredentialRequestOptions(String w3cCredentialRequestOptions) {
        this.w3cCredentialRequestOptions = w3cCredentialRequestOptions;
    }

    public String getW3cAuthenticatorAssertionResponse() {
        return w3cAuthenticatorAssertionResponse;
    }

    public void setW3cAuthenticatorAssertionResponse(String w3cAuthenticatorAssertionResponse) {
        this.w3cAuthenticatorAssertionResponse = w3cAuthenticatorAssertionResponse;
    }

    public String getUserVerificationOption() {
        return userVerificationOption;
    }

    public void setUserVerificationOption(String userVerificationOption) {
        this.userVerificationOption = userVerificationOption;
    }

}
