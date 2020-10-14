/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.cert.validation;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import io.jans.as.common.cert.validation.model.ValidationStatus;

/**
 * Base interface for all certificate verifiers
 * 
 * @author Yuriy Movchan
 * @version March 11, 2016
 */
public interface CertificateVerifier {

	public abstract ValidationStatus validate(X509Certificate certificate, List<X509Certificate> issuers, Date validationDate);

	public abstract void destroy();

}