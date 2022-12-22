/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.jans.as.model.util.Base64Util;
import io.jans.fido2.model.u2f.util.CertificateParser;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * U2F Device registration key
 *
 * @author Yuriy Movchan Date: 05/29/2015
 */
public class DeviceRegistrationConfiguration {

    @JsonProperty
    public final String publicKey;

    @JsonProperty
    public final String attestationCert;

    public DeviceRegistrationConfiguration(@JsonProperty("publicKey") String publicKey,
                                           @JsonProperty("attestationCert") String attestationCert) {
        this.publicKey = publicKey;
        this.attestationCert = attestationCert;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getAttestationCert() {
        return attestationCert;
    }

    @JsonIgnore
    public X509Certificate getAttestationCertificate() throws CertificateException, NoSuchFieldException {
        if (attestationCert == null) {
            throw new NoSuchFieldException();
        }
        return CertificateParser.parseDer(Base64Util.base64urldecode(attestationCert));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeviceRegistrationConfiguration [publicKey=").append(publicKey).append(", attestationCert=").append(attestationCert).append("]");
        return builder.toString();
    }

}