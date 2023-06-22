/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.service.verifier;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import io.jans.fido2.model.mds.AuthenticatorCertificationStatus;
import io.jans.fido2.service.CertificateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.fido2.exception.Fido2MissingAttestationCertException;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.service.Base64Service;
import org.slf4j.Logger;

@ApplicationScoped
public class CertificateVerifier {

    @Inject
    private Logger log;

    @Inject
    private Base64Service base64Service;

    @Inject
    private CertificateService certificateService;

    public void checkForTrustedCertsInAttestation(List<X509Certificate> attestationCerts, List<X509Certificate> trustChainCertificates) {
        final List<String> trustedSignatures = trustChainCertificates.stream().map(cert -> base64Service.encodeToString(cert.getSignature()))
                .collect(Collectors.toList());
        List<String> duplicateSignatures = attestationCerts.stream().map(cert -> base64Service.encodeToString(cert.getSignature()))
                .filter(trustedSignatures::contains).collect(Collectors.toList());
        if (!duplicateSignatures.isEmpty()) {
            throw new Fido2RuntimeException("Root certificate in the attestation");
        }
    }

    public X509Certificate verifyAttestationCertificates(List<X509Certificate> certs, List<X509Certificate> trustChainCertificates) {
        try {
            checkForTrustedCertsInAttestation(certs, trustChainCertificates);
            Set<TrustAnchor> trustAnchors = trustChainCertificates.parallelStream().map(f -> new TrustAnchor(f, null)).collect(Collectors.toSet());

            if (trustAnchors.isEmpty()) {
                throw new Fido2MissingAttestationCertException("Trust anchors certs list is empty!");
            }

            PKIXParameters params = new PKIXParameters(trustAnchors);
            CertPathValidator cpv = certificateService.instanceCertPathValidatorPKIX();

            PKIXRevocationChecker rc = (PKIXRevocationChecker) cpv.getRevocationChecker();
            rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL, PKIXRevocationChecker.Option.PREFER_CRLS));
            params.addCertPathChecker(rc);

            CertificateFactory certFactory = certificateService.instanceCertificateFactoryX509();
            CertPath certPath = certFactory.generateCertPath(certs);

            X509Certificate cert = verifyPath(cpv, certPath, params);
            if (cert != null) {
                return cert;
            } else {
                params = new PKIXParameters(trustAnchors);
                cpv = certificateService.instanceCertPathValidatorPKIX();
                rc = (PKIXRevocationChecker) cpv.getRevocationChecker();
                rc.setOptions(Collections.emptySet());
                params.setRevocationEnabled(false);
                params.addCertPathChecker(null);

                return verifyPath(cpv, certPath, params);
            }
        } catch (InvalidAlgorithmParameterException | CertificateException e) {
            log.warn("Cert verification problem {}", e.getMessage(), e);
            throw new Fido2RuntimeException("Problem with certificate");
        }
    }

    private X509Certificate verifyPath(CertPathValidator cpv, CertPath certPath, PKIXParameters params) {
    	if (certPath.getCertificates().size() == 0) {
    		return null;
    	}

    	try {
            cpv.validate(certPath, params);
            return (X509Certificate) certPath.getCertificates().get(0);
        } catch (CertPathValidatorException ex) {
            if (ex.getReason() == CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS) {
                log.warn("Cert not validated against the root {}", ex.getMessage());
                return null;
            } else {
                log.error("Cert not validated against the root {}", ex.getMessage());
                throw new Fido2RuntimeException("Problem with certificate " + ex.getMessage());
            }
        } catch (InvalidAlgorithmParameterException e) {
            log.warn("Cert verification problem {}", e.getMessage(), e);
            throw new Fido2RuntimeException("Problem with certificate");
        }
    }

    public boolean isSelfSigned(X509Certificate cert) {
        return isSelfSigned(cert, cert.getPublicKey());
    }

    public boolean isSelfSigned(X509Certificate cert, PublicKey key) {
        try {
            // Try to verify certificate signature with its own public key
            cert.verify(key);
            return cert.getIssuerDN().equals(cert.getSubjectDN());
        } catch (SignatureException | CertificateException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e) {
            log.warn("Probably not self signed cert. Cert verification problem {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify that the MDS entry contains a valid state
     *
     * @param aaguid AAGUID from MetadataBlobEntry
     * @param metadataEntry MetadataBlobEntry
     * @throws Fido2RuntimeException If it contains errors
     */
    public void verifyStatusAcceptable(String aaguid, JsonNode metadataEntry) throws Fido2RuntimeException {
        if (!metadataEntry.has("statusReports")) {
            throw new Fido2RuntimeException(String.format("Ignore entry AAGUID: %s due it does not contain 'statusReports' field", aaguid));
        }
        JsonNode statusReportsNode = metadataEntry.get("statusReports");
        if (statusReportsNode.isEmpty()) {
            throw new Fido2RuntimeException(String.format("Ignore entry AAGUID: %s because 'statusReports' doesn't contain values", aaguid));
        }
        for (JsonNode statusNode : metadataEntry.get("statusReports")) {
            if (!statusNode.has("status")) {
                throw new Fido2RuntimeException(String.format("Ignore entry AAGUID: %s due it does not contain 'status' field", aaguid));
            }
            AuthenticatorCertificationStatus authenticatorStatus = AuthenticatorCertificationStatus.valueOf(statusNode.get("status").asText());
            String authenticatorEffectiveDate = statusNode.get("effectiveDate").asText();
            log.debug("Authenticator AAGUID {} status {} effective date {}", aaguid, authenticatorStatus, authenticatorEffectiveDate);
            if (!isAcceptableStatus(authenticatorStatus)) {
                throw new Fido2RuntimeException(String.format("Ignore entry AAGUID: %s due to status: %s", aaguid, authenticatorStatus.name()));
            }
        }
    }

    /**
     * Verify that the status is valid
     *
     * @param authenticatorStatus {@link AuthenticatorCertificationStatus} status
     * @return true if valid, false if invalid
     */
    private boolean isAcceptableStatus(AuthenticatorCertificationStatus authenticatorStatus) {
        return authenticatorStatus != AuthenticatorCertificationStatus.USER_VERIFICATION_BYPASS &&
                authenticatorStatus != AuthenticatorCertificationStatus.ATTESTATION_KEY_COMPROMISE &&
                authenticatorStatus != AuthenticatorCertificationStatus.USER_KEY_REMOTE_COMPROMISE &&
                authenticatorStatus != AuthenticatorCertificationStatus.USER_KEY_PHYSICAL_COMPROMISE &&
                authenticatorStatus != AuthenticatorCertificationStatus.REVOKED;
    }
}
