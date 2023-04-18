/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.cert.validation;

import io.jans.as.common.cert.validation.model.ValidationStatus;
import io.jans.util.security.SecurityProviderUtility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

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
        ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidationStatus.ValidatorSourceType.APP, ValidationStatus.CertificateValidity.UNKNOWN);

        try {
            Principal subjectX500Principal = certificate.getSubjectX500Principal();

            try {
                log.debug("Validity status is valid for '" + subjectX500Principal + "'");
                certificate.checkValidity(validationDate);
                status.setValidity(ValidationStatus.CertificateValidity.VALID);
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
