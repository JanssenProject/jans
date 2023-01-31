/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.cert.validation;

import io.jans.as.common.cert.validation.model.ValidationStatus;
import io.jans.util.security.SecurityProviderUtility;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Chain certificate verifier
 *
 * @author Yuriy Movchan
 * @version March 11, 2016
 */
public class PathCertificateVerifier implements CertificateVerifier {

    private static final Logger log = LoggerFactory.getLogger(PathCertificateVerifier.class);

    private final boolean verifySelfSignedCertificate;

    public PathCertificateVerifier(boolean verifySelfSignedCert) {
        SecurityProviderUtility.installBCProvider(true);

        this.verifySelfSignedCertificate = verifySelfSignedCert;
    }

    public static boolean isSelfSigned(X509Certificate certificate) throws CertificateException, NoSuchAlgorithmException, NoSuchProviderException {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = certificate.getPublicKey();
            certificate.verify(key);

            return true;
        } catch (SignatureException | InvalidKeyException ex) {
            // Not self-signed
            return false;
        }
    }

    @Override
    public ValidationStatus validate(X509Certificate certificate, List<X509Certificate> issuers, Date validationDate) {
        X509Certificate issuer = issuers.get(0);
        ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidationStatus.ValidatorSourceType.CHAIN, ValidationStatus.CertificateValidity.UNKNOWN);

        try {
            ArrayList<X509Certificate> chains = new ArrayList<>();
            chains.add(certificate);
            chains.addAll(issuers);

            Principal subjectX500Principal = certificate.getSubjectX500Principal();

            PKIXCertPathBuilderResult certPathResult = verifyCertificate(certificate, chains);
            if (certPathResult == null) {
                log.warn("Chain status is not valid for '" + subjectX500Principal + "'");
                status.setValidity(ValidationStatus.CertificateValidity.INVALID);
                return status;
            }

            log.debug("Chain status is valid for '" + subjectX500Principal + "'");
            status.setValidity(ValidationStatus.CertificateValidity.VALID);
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
            Set<X509Certificate> trustedRootCerts = new HashSet<>();
            Set<X509Certificate> intermediateCerts = new HashSet<>();
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
        } catch (GeneralSecurityException ex) {
            log.error("Failed to build certificate path", ex);
        }

        return null;
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
        Set<TrustAnchor> trustAnchors = new HashSet<>();
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
        certPathValidator.validate(certPathBuilderResult.getCertPath(), pkixParams);

        return certPathBuilderResult;
    }

    @Override
    public void destroy() {
        // empty destroy
    }

}
