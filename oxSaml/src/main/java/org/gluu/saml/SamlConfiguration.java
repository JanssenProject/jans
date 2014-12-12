/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.saml;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.exception.CloneFailedException;
import org.xdi.util.security.CertificateHelper;

/**
 * Configuration settings
 * 
 * @author Yuriy Movchan Date: 24/04/2014
 */
public class SamlConfiguration {

	private String idpSsoTargetUrl;
	private String assertionConsumerServiceUrl;
	private String issuer;
	private String nameIdentifierFormat;
	private X509Certificate certificate;
	private boolean useRequestedAuthnContext;

	public String getIdpSsoTargetUrl() {
		return idpSsoTargetUrl;
	}

	public void setIdpSsoTargetUrl(String idpSsoTargetUrl) {
		this.idpSsoTargetUrl = idpSsoTargetUrl;
	}

	public String getAssertionConsumerServiceUrl() {
		return assertionConsumerServiceUrl;
	}

	public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
		this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getNameIdentifierFormat() {
		return nameIdentifierFormat;
	}

	public void setNameIdentifierFormat(String nameIdentifierFormat) {
		this.nameIdentifierFormat = nameIdentifierFormat;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	public void setCertificate(X509Certificate certificate) {
		this.certificate = certificate;
	}

	public boolean isUseRequestedAuthnContext() {
		return useRequestedAuthnContext;
	}

	public void setUseRequestedAuthnContext(boolean useRequestedAuthnContext) {
		this.useRequestedAuthnContext = useRequestedAuthnContext;
	}

	public void loadCertificateFromString(String certificateString) throws CertificateException {
		this.certificate = CertificateHelper.loadCertificate(certificateString);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		try {
			return BeanUtils.cloneBean(this);
		} catch (Exception ex) {
			throw new CloneFailedException(ex);
		}
	}

}
