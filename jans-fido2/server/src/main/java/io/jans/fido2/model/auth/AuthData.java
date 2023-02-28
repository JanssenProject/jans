/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.model.auth;

/**
 * authData structure from https://www.w3.org/TR/webauthn/#authenticator-data
 * @author Yuriy Movchan
 * @version March 9, 2020
 */
public class AuthData {
    private byte[] rpIdHash;
    private byte[] flags;
    private byte[] counters;
    private byte[] aaguid;
    private byte[] credId;
    private byte[] attestationBuffer;
    private int keyType;
    private byte[] cosePublicKey;
    private byte[] extensions;

    private byte[] authDataDecoded;


    public byte[] getRpIdHash() {
        return rpIdHash;
    }

    public AuthData setRpIdHash(byte[] rpIdHash) {
        this.rpIdHash = rpIdHash;
        return this;
    }

    public byte[] getFlags() {
        return flags;
    }

    public AuthData setFlags(byte[] flags) {
        this.flags = flags;
        return this;
    }

    public byte[] getCounters() {
        return counters;
    }

    public AuthData setCounters(byte[] counters) {
        this.counters = counters;
        return this;
    }

    public byte[] getAaguid() {
        return aaguid;
    }

    public AuthData setAaguid(byte[] aaguid) {
        this.aaguid = aaguid;
        return this;
    }

    public byte[] getCredId() {
        return credId;
    }

    public AuthData setCredId(byte[] credId) {
        this.credId = credId;
        return this;
    }

    public byte[] getCosePublicKey() {
        return cosePublicKey;
    }

    public AuthData setCosePublicKey(byte[] cosePublicKey) {
        this.cosePublicKey = cosePublicKey;
        return this;
    }

    public byte[] getAttestationBuffer() {
        return attestationBuffer;
    }

    public void setAttestationBuffer(byte[] attestationBuffer) {
        this.attestationBuffer = attestationBuffer;
    }

    public int getKeyType() {
        return keyType;
    }

    public void setKeyType(int keyType) {
        this.keyType = keyType;
    }

    public byte[] getAuthDataDecoded() {
        return authDataDecoded;
    }

    public void setAuthDataDecoded(byte[] authDataDecoded) {
        this.authDataDecoded = authDataDecoded;
    }

	public byte[] getExtensions() {
		return extensions;
	}

	public AuthData setExtensions(byte[] extensions) {
		this.extensions = extensions;
        return this;
	}

}
