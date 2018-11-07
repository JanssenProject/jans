/*
 * Copyright (c) 2018 Mastercard
 * Copyright (c) 2018 Gluu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.gluu.oxauth.fido2.service;

import java.io.File;
import java.io.IOException;
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
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

@Named
@Stateless
public class CertificateValidator {

    @Inject
    private Logger log;

    @Inject
    @Named("base64Encoder")
    private Base64.Encoder base64Encoder;

    void saveCertificate(X509Certificate certificate) throws IOException {
        FileUtils.writeStringToFile(new File("c:/tmp/cert-" + certificate.getSerialNumber() + ".crt"), certificate.toString());
    }

    void checkForTrustedCertsInAttestation(List<X509Certificate> attestationCerts, List<X509Certificate> trustChainCertificates) {
        final List<String> trustedSignatures = trustChainCertificates.stream().map(cert -> base64Encoder.encodeToString(cert.getSignature()))
                .collect(Collectors.toList());
        List<String> duplicateSignatures = attestationCerts.stream().map(cert -> base64Encoder.encodeToString(cert.getSignature()))
                .filter(sig -> trustedSignatures.contains(sig)).collect(Collectors.toList());
        if (!duplicateSignatures.isEmpty()) {
            throw new Fido2RPRuntimeException("Root certificate in the attestation ");
        }
    }

    public X509Certificate verifyAttestationCertificates(List<X509Certificate> certs, List<X509Certificate> trustChainCertificates) {
        try {
            checkForTrustedCertsInAttestation(certs, trustChainCertificates);
            Set<TrustAnchor> trustAnchors = trustChainCertificates.parallelStream().map(f -> new TrustAnchor(f, null)).collect(Collectors.toSet());

            if (trustAnchors.isEmpty()) {
                log.warn("Empty list of trust managers");
                return (X509Certificate) certs.get(0);
            }

            PKIXParameters params = new PKIXParameters(trustAnchors);
            CertPathValidator cpv = CertPathValidator.getInstance("PKIX");

            PKIXRevocationChecker rc = (PKIXRevocationChecker) cpv.getRevocationChecker();
            rc.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL, PKIXRevocationChecker.Option.PREFER_CRLS));
            params.addCertPathChecker(rc);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            CertPath certPath = certFactory.generateCertPath(certs);

            X509Certificate cert = verifyPath(cpv, certPath, params);
            if (cert != null) {
                return cert;
            } else {
                params = new PKIXParameters(trustAnchors);
                cpv = CertPathValidator.getInstance("PKIX");
                rc = (PKIXRevocationChecker) cpv.getRevocationChecker();
                rc.setOptions(Collections.emptySet());
                params.setRevocationEnabled(false);
                params.addCertPathChecker(null);

                return verifyPath(cpv, certPath, params);
            }

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | CertificateException e) {
            log.warn("Cert verification problem {}", e.getMessage(), e);
            throw new Fido2RPRuntimeException("Problem with certificate");
        }
    }

    private X509Certificate verifyPath(CertPathValidator cpv, CertPath certPath, PKIXParameters params) {
        try {
            cpv.validate(certPath, params);
            return (X509Certificate) certPath.getCertificates().get(0);
        } catch (CertPathValidatorException ex) {
            if (ex.getReason() == CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS) {
                log.info("Cert not validated against the root {}", ex.getMessage());
                return null;
            } else {
                log.warn("Cert not validated against the root {}", ex.getMessage());
                throw new Fido2RPRuntimeException("Problem with certificate " + ex.getMessage());
            }
        } catch (InvalidAlgorithmParameterException e) {
            log.warn("Cert verification problem {}", e.getMessage(), e);
            throw new Fido2RPRuntimeException("Problem with certificate");
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
}
