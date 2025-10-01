/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.cert.validation;

import io.jans.as.common.cert.validation.model.ValidationStatus;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

/**
 * Base interface for all certificate verifiers
 *
 * @author Yuriy Movchan
 * @version March 11, 2016
 */
public interface CertificateVerifier {

    ValidationStatus validate(X509Certificate certificate, List<X509Certificate> issuers, Date validationDate);

    void destroy();

}