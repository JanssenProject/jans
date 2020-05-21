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

public class Fido2RegistrationData extends Fido2Data {

    private static final long serialVersionUID = 4599467930864459334L;

    private String username;
    private String domain;
    private String userId;
    private String challenge;

    private String attenstationRequest;
    private String attenstationResponse;

    private String uncompressedECPoint;
    private String publicKeyId;

    private String type;

    private Fido2RegistrationStatus status;

    private int counter;

    private String attestationType;

    private int signatureAlgorithm;

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

    public String getAttenstationRequest() {
        return attenstationRequest;
    }

    public void setAttenstationRequest(String attenstationRequest) {
        this.attenstationRequest = attenstationRequest;
    }

    public String getAttenstationResponse() {
        return attenstationResponse;
    }

    public void setAttenstationResponse(String attenstationResponse) {
        this.attenstationResponse = attenstationResponse;
    }

    public String getUncompressedECPoint() {
        return uncompressedECPoint;
    }

    public void setUncompressedECPoint(String uncompressedECPoint) {
        this.uncompressedECPoint = uncompressedECPoint;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Fido2RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(Fido2RegistrationStatus status) {
        this.status = status;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public String getAttestationType() {
        return attestationType;
    }

    public void setAttestationType(String attestationType) {
        this.attestationType = attestationType;
    }

    public int getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(int signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    @Override
	public String toString() {
		return "Fido2RegistrationData [username=" + username + ", domain=" + domain + ", userId=" + userId + ", challenge=" + challenge
				+ ", attenstationRequest=" + attenstationRequest + ", attenstationResponse=" + attenstationResponse
				+ ", uncompressedECPoint=" + uncompressedECPoint + ", publicKeyId=" + publicKeyId + ", type=" + type + ", status=" + status
				+ ", counter=" + counter + ", attestationType=" + attestationType + ", signatureAlgorithm=" + signatureAlgorithm + "]";
	}
}
