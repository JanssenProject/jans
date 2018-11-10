/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.gluu.oxauth.fido2.google.safetynet;

import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Base64;
import com.google.api.client.util.Key;

/**
 * A statement returned by the Attestation API.
 */
public class AttestationStatement extends JsonWebSignature.Payload {

    /**
     * Embedded nonce sent as part of the request.
     */
    @Key
    private String nonce;

    /**
     * Timestamp of the request.
     */
    @Key
    private long timestampMs;

    /**
     * Package name of the APK that submitted this request.
     */
    @Key
    private String apkPackageName;

    /**
     * Digest of certificate of the APK that submitted this request.
     */
    @Key
    private String[] apkCertificateDigestSha256;

    /**
     * Digest of the APK that submitted this request.
     */
    @Key
    private String apkDigestSha256;

    /**
     * The device passed CTS and matches a known profile.
     */
    @Key
    private boolean ctsProfileMatch;

    /**
     * The device has passed a basic integrity test, but the CTS profile could not
     * be verified.
     */
    @Key
    private boolean basicIntegrity;

    public byte[] getNonce() {
        return Base64.decodeBase64(nonce);
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public String getApkPackageName() {
        return apkPackageName;
    }

    public byte[] getApkDigestSha256() {
        return Base64.decodeBase64(apkDigestSha256);
    }

    public byte[][] getApkCertificateDigestSha256() {
        byte[][] certs = new byte[apkCertificateDigestSha256.length][];
        for (int i = 0; i < apkCertificateDigestSha256.length; i++) {
            certs[i] = Base64.decodeBase64(apkCertificateDigestSha256[i]);
        }
        return certs;
    }

    public boolean isCtsProfileMatch() {
        return ctsProfileMatch;
    }

    public boolean hasBasicIntegrity() {
        return basicIntegrity;
    }
}
