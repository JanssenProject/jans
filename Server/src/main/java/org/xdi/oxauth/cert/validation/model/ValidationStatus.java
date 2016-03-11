/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.cert.validation.model;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Certificate validation status
 * 
 * @author Yuriy Movchan
 * @version March 10, 2016
 */
public class ValidationStatus {

	private X509Certificate certificate;
	private X509Certificate issuer;

	private CertificateValidity validity;
	private ValidatorSourceType sourceType;

	private Date revocationObjectIssuingTime;
	private Date revocationDate;
	private Date validationDate;

	public ValidationStatus(X509Certificate certificate, X509Certificate issuer, Date validationDate, ValidatorSourceType sourceType,
			CertificateValidity validity) {
		this.certificate = certificate;
		this.issuer = issuer;
		this.validationDate = validationDate;
		this.sourceType = sourceType;
		this.validity = validity;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	public void setCertificate(X509Certificate certificate) {
		this.certificate = certificate;
	}

	public X509Certificate getIssuer() {
		return issuer;
	}

	public void setIssuer(X509Certificate issuer) {
		this.issuer = issuer;
	}

	public CertificateValidity getValidity() {
		return validity;
	}

	public void setValidity(CertificateValidity validity) {
		this.validity = validity;
	}

	public ValidatorSourceType getSourceType() {
		return sourceType;
	}

	public void setSourceType(ValidatorSourceType sourceType) {
		this.sourceType = sourceType;
	}

	public Date getRevocationObjectIssuingTime() {
		return revocationObjectIssuingTime;
	}

	public void setRevocationObjectIssuingTime(Date revocationObjectIssuingTime) {
		this.revocationObjectIssuingTime = revocationObjectIssuingTime;
	}

	public Date getRevocationDate() {
		return revocationDate;
	}

	public void setRevocationDate(Date revocationDate) {
		this.revocationDate = revocationDate;
	}

	public Date getValidationDate() {
		return validationDate;
	}

	public void setValidationDate(Date validationDate) {
		this.validationDate = validationDate;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CertificateValidationStatus [certificate=").append(certificate.getSerialNumber()).append(", issuer=").append(issuer.getSerialNumber()).append(", validity=")
				.append(validity).append(", sourceType=").append(sourceType).append(", revocationObjectIssuingTime=")
				.append(revocationObjectIssuingTime).append(", revocationDate=").append(revocationDate).append(", validationDate=").append(validationDate)
				.append("]");
		return builder.toString();
	}

	public enum ValidatorSourceType {
		OCSP, CRL, APP, CHAIN, SELF_SIGNED
	}

	public enum CertificateValidity {
		VALID, INVALID, REVOKED, UNKNOWN
	}

}
