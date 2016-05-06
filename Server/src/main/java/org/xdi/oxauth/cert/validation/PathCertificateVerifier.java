/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.cert.validation;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.xdi.oxauth.cert.validation.model.ValidationStatus;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.CertificateValidity;
import org.xdi.oxauth.cert.validation.model.ValidationStatus.ValidatorSourceType;
import org.xdi.oxauth.model.util.SecurityProviderUtility;

/**
 * Chain certificate verifier
 * 
 * @author Yuriy Movchan
 * @version March 11, 2016
 */
public class PathCertificateVerifier implements CertificateVerifier {

	private static final Logger log = Logger.getLogger(PathCertificateVerifier.class);

	private boolean verifySelfSignedCertificate;

	public PathCertificateVerifier(boolean verifySelfSignedCert) {
		SecurityProviderUtility.installBCProvider(true);

		this.verifySelfSignedCertificate = verifySelfSignedCert;
	}

	@Override
	public ValidationStatus validate(X509Certificate certificate, List<X509Certificate> issuers, Date validationDate) {
		X509Certificate issuer = issuers.get(0);
		ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidatorSourceType.CHAIN, CertificateValidity.UNKNOWN);

		try {
			ArrayList<X509Certificate> chains = new ArrayList<X509Certificate>();
			chains.add(certificate);
			chains.addAll(issuers);

			Principal subjectX500Principal = certificate.getSubjectX500Principal();

			PKIXCertPathBuilderResult certPathResult = verifyCertificate(certificate, chains);
			if (certPathResult == null) {
				log.warn("Chain status is not valid for '" + subjectX500Principal + "'");
				status.setValidity(CertificateValidity.INVALID);
				return status;
			}

			log.debug("Chain status is valid for '" + subjectX500Principal + "'");
			status.setValidity(CertificateValidity.VALID);
		} catch (Exception ex) {
			log.error("OCSP exception: ", ex);
		}

		return status;
	}

	public PKIXCertPathBuilderResult verifyCertificate(X509Certificate certificate, List<X509Certificate> additionalCerts) {
		try {
			// Check for self-signed certificate
			if (!verifySelfSignedCertificate && isSelfSigned(certificate)) {
				log.error("The certificate is self-signed!");

				return null;
			}

			// Prepare a set of trusted root CA certificates and a set of
			// intermediate certificates
			Set<X509Certificate> trustedRootCerts = new HashSet<X509Certificate>();
			Set<X509Certificate> intermediateCerts = new HashSet<X509Certificate>();
			for (X509Certificate additionalCert : additionalCerts) {
				if (isSelfSigned(additionalCert)) {
					trustedRootCerts.add(additionalCert);
				} else {
					intermediateCerts.add(additionalCert);
				}
			}

			// Attempt to build the certification chain and verify it
			PKIXCertPathBuilderResult certPathBuilderResult = verifyCertificate(certificate, trustedRootCerts, intermediateCerts);

			// Check that first certificate is an EE certificate
			CertPath certPath = certPathBuilderResult.getCertPath();
			List<? extends Certificate> certList = certPath.getCertificates();
			X509Certificate cert = (X509Certificate) certList.get(0);
			if (cert.getBasicConstraints() != -1) {
				log.error("Target certificate is not an EE certificate!");

				return null;
			}

			// The chain is verified. Return it as a result
			return certPathBuilderResult;
		} catch (CertPathBuilderException ex) {
			log.error("Failed to build certificate path", ex);
		} catch (GeneralSecurityException ex) {
			log.error("Failed to build certificate path", ex);
		}

		return null;
	}

	public static boolean isSelfSigned(X509Certificate certificate) throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
		try {
			// Try to verify certificate signature with its own public key
			PublicKey key = certificate.getPublicKey();
			certificate.verify(key);

			return true;
		} catch (SignatureException ex) {
			// Not self-signed
			return false;
		} catch (InvalidKeyException ex) {
			// Not self-signed
			return false;
		}
	}

	/**
	 * Attempts to build a certification chain for given certificate to verify
	 * it. Relies on a set of root CA certificates (trust anchors) and a set of
	 * intermediate certificates (to be used as part of the chain).
	 */
	private PKIXCertPathBuilderResult verifyCertificate(X509Certificate certificate, Set<X509Certificate> trustedRootCerts, Set<X509Certificate> intermediateCerts)
			throws GeneralSecurityException {

		// Create the selector that specifies the starting certificate
		X509CertSelector selector = new X509CertSelector();
		selector.setBasicConstraints(-2);
		selector.setCertificate(certificate);

		// Create the trust anchors (set of root CA certificates)
		Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
		for (X509Certificate trustedRootCert : trustedRootCerts) {
			trustAnchors.add(new TrustAnchor(trustedRootCert, null));
		}

		// Configure the PKIX certificate builder algorithm parameters
		PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

		// Turn off default revocation-checking mechanism
		pkixParams.setRevocationEnabled(false);

		// Specify a list of intermediate certificates
		CertStore intermediateCertStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(intermediateCerts));
		pkixParams.addCertStore(intermediateCertStore);

		// Build and verify the certification chain
		CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
		PKIXCertPathBuilderResult certPathBuilderResult = (PKIXCertPathBuilderResult) builder.build(pkixParams);

		// Additional check to Verify cert path
		CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX", BouncyCastleProvider.PROVIDER_NAME);
		PKIXCertPathValidatorResult certPathValidationResult = (PKIXCertPathValidatorResult) certPathValidator.validate(certPathBuilderResult.getCertPath(), pkixParams);

		return certPathBuilderResult;
	}

	@Override
	public void destroy() {
	}

}
