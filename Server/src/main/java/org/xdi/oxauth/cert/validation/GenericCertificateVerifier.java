/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.cert.validation;

import java.security.Principal;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.cert.validation.model.ValidationStatus;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.CertificateValidity;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.ValidatorSourceType;
import org.xdi.oxauth.model.util.SecurityProviderUtility;

/**
 * Certificate verifier based on CRL
 * 
 * @author Yuriy Movchan
 * @version March 11, 2016
 */
public class GenericCertificateVerifier implements CertificateVerifier {

	private static final Logger log = LoggerFactory.getLogger(GenericCertificateVerifier.class);

	public GenericCertificateVerifier() {
		SecurityProviderUtility.installBCProvider(true);
	}

	@Override
	public ValidationStatus validate(X509Certificate certificate, List<X509Certificate> issuers, Date validationDate) {
		X509Certificate issuer = issuers.get(0);
		ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidatorSourceType.APP, CertificateValidity.UNKNOWN);

		try {
			Principal subjectX500Principal = certificate.getSubjectX500Principal();

			try {
				log.debug("Validity status is valid for '" + subjectX500Principal + "'");
				certificate.checkValidity(validationDate);
				status.setValidity(CertificateValidity.VALID);
			} catch (CertificateExpiredException ex) {
				log.debug("Validity status is expied for '" + subjectX500Principal + "'");
			} catch (CertificateNotYetValidException ex) {
				log.warn("Validity status is not yet valid for '" + subjectX500Principal + "'");
			}
		} catch (Exception ex) {
			log.error("CRL exception: ", ex);
		}

		return status;
	}

	@Override
	public void destroy() {
	}

}
