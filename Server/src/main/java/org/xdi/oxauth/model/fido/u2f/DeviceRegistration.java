/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.oxauth.model.fido.u2f;

import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.oxauth.crypto.cert.CertificateParser;
import org.xdi.oxauth.exception.fido.u2f.InvalidDeviceCounterException;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.util.Base64Util;

/**
 * U2F Device registration
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
public class DeviceRegistration implements Serializable {

	private static final long serialVersionUID = -4542931562244920584L;

	@JsonProperty
	private final String keyHandle;

	@JsonProperty
	private final String publicKey;

	@JsonProperty
	private final String attestationCert;

	@JsonProperty
	private long counter;

	@JsonProperty
	private boolean compromised;

	public DeviceRegistration(@JsonProperty("keyHandle") String keyHandle, @JsonProperty("publicKey") String publicKey,
			@JsonProperty("attestationCert") String attestationCert, @JsonProperty("counter") long counter, @JsonProperty("compromised") boolean compromised) {
		this.keyHandle = keyHandle;
		this.publicKey = publicKey;
		this.attestationCert = attestationCert;
		this.counter = counter;
		this.compromised = compromised;
	}

	public DeviceRegistration(String keyHandle, String publicKey, X509Certificate attestationCert, long counter) throws BadInputException {
		this.keyHandle = keyHandle;
		this.publicKey = publicKey;
		try {
			this.attestationCert = Base64Util.base64urlencode(attestationCert.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new BadInputException("Malformed attestation certificate", e);
		}
		this.counter = counter;
	}

	public String getKeyHandle() {
		return keyHandle;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public X509Certificate getAttestationCertificate() throws CertificateException, NoSuchFieldException {
		if (attestationCert == null) {
			throw new NoSuchFieldException();
		}
		return CertificateParser.parseDer(Base64Util.base64urldecode(attestationCert));
	}

	public long getCounter() {
		return counter;
	}

	public boolean isCompromised() {
		return compromised;
	}

	public void markCompromised() {
		compromised = true;
	}

	public void checkAndUpdateCounter(long clientCounter) throws InvalidDeviceCounterException {
		if (clientCounter <= counter) {
			markCompromised();
			throw new InvalidDeviceCounterException(this);
		}
		counter = clientCounter;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DeviceRegistration [keyHandle=").append(keyHandle).append(", publicKey=").append(publicKey).append(", attestationCert=")
				.append(attestationCert).append(", counter=").append(counter).append(", compromised=").append(compromised).append("]");
		return builder.toString();
	}

}
