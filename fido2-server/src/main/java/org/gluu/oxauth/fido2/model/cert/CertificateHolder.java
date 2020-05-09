package org.gluu.oxauth.fido2.model.cert;

import java.security.cert.Certificate;

public class CertificateHolder {

	private final String alias;
	private final Certificate cert;

	public CertificateHolder(String alias, Certificate cert) {
		this.alias = alias;
		this.cert = cert;
	}

	public String getAlias() {
		return alias;
	}

	public Certificate getCert() {
		return cert;
	}

}
