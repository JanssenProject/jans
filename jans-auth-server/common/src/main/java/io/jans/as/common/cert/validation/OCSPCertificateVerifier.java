/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.cert.validation;

import io.jans.as.common.cert.validation.model.ValidationStatus;
import io.jans.util.security.SecurityProviderUtility;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

/**
 * Certificate verifier based on OCSP
 *
 * @author Yuriy Movchan
 * @version March 10, 2016
 */
public class OCSPCertificateVerifier implements CertificateVerifier {

    private static final Logger log = LoggerFactory.getLogger(OCSPCertificateVerifier.class);

    public OCSPCertificateVerifier() {
        SecurityProviderUtility.installBCProvider(true);
    }

    /**
     * @param certificate the certificate from which we need the ExtensionValue
     * @param oid         the Object Identifier value for the extension.
     * @return the extension value as an ASN1Primitive object
     * @throws IOException
     */
    private static ASN1Primitive getExtensionValue(X509Certificate certificate, String oid) throws IOException {
        byte[] bytes = certificate.getExtensionValue(oid);
        if (bytes == null) {
            return null;
        }
        ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(bytes));
        ASN1OctetString octs = (ASN1OctetString) aIn.readObject();
        aIn = new ASN1InputStream(new ByteArrayInputStream(octs.getOctets()));
        return aIn.readObject();
    }

    @Override
    public ValidationStatus validate(X509Certificate certificate, List<X509Certificate> issuers, Date validationDate) {
        X509Certificate issuer = issuers.get(0);
        ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidationStatus.ValidatorSourceType.OCSP, ValidationStatus.CertificateValidity.UNKNOWN);

        try {
            Principal subjectX500Principal = certificate.getSubjectX500Principal();

            String ocspUrl = getOCSPUrl(certificate);
            if (ocspUrl == null) {
                log.error("OCSP URL for '" + subjectX500Principal + "' is empty");
                return status;
            }

            log.debug("OCSP URL for '" + subjectX500Principal + "' is '" + ocspUrl + "'");

            DigestCalculator digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
            CertificateID certificateId = new CertificateID(digestCalculator, new JcaX509CertificateHolder(certificate), certificate.getSerialNumber());

            // Generate OCSP request
            OCSPReq ocspReq = generateOCSPRequest(certificateId);

            // Get OCSP response from server
            OCSPResp ocspResp = requestOCSPResponse(ocspUrl, ocspReq);
            if (ocspResp.getStatus() != OCSPRespBuilder.SUCCESSFUL) {
                log.error("OCSP response is invalid!");
                status.setValidity(ValidationStatus.CertificateValidity.INVALID);
                return status;
            }

            boolean foundResponse = false;
            BasicOCSPResp basicOCSPResp = (BasicOCSPResp) ocspResp.getResponseObject();
            SingleResp[] singleResps = basicOCSPResp.getResponses();
            for (SingleResp singleResp : singleResps) {
                CertificateID responseCertificateId = singleResp.getCertID();
                if (!certificateId.equals(responseCertificateId)) {
                    continue;
                }

                foundResponse = true;

                log.debug("OCSP validationDate: " + validationDate);
                log.debug("OCSP thisUpdate: " + singleResp.getThisUpdate());
                log.debug("OCSP nextUpdate: " + singleResp.getNextUpdate());

                status.setRevocationObjectIssuingTime(basicOCSPResp.getProducedAt());

                Object certStatus = singleResp.getCertStatus();
                if (certStatus == CertificateStatus.GOOD) {
                    log.debug("OCSP status is valid for '" + certificate.getSubjectX500Principal() + "'");
                    status.setValidity(ValidationStatus.CertificateValidity.VALID);
                } else {
                    if (singleResp.getCertStatus() instanceof RevokedStatus) {
                        log.warn("OCSP status is revoked for: " + subjectX500Principal);
                        if (validationDate.before(((RevokedStatus) singleResp.getCertStatus()).getRevocationTime())) {
                            log.warn("OCSP revocation time after the validation date, the certificate '" + subjectX500Principal + "' was valid at " + validationDate);
                            status.setValidity(ValidationStatus.CertificateValidity.VALID);
                        } else {
                            Date revocationDate = ((RevokedStatus) singleResp.getCertStatus()).getRevocationTime();
                            log.info("OCSP for certificate '" + subjectX500Principal + "' is revoked since " + revocationDate);
                            status.setRevocationDate(revocationDate);
                            status.setRevocationObjectIssuingTime(singleResp.getThisUpdate());
                            status.setValidity(ValidationStatus.CertificateValidity.REVOKED);
                        }
                    }
                }
            }

            if (!foundResponse) {
                log.error("There is no matching OCSP response entries");
            }
        } catch (Exception ex) {
            log.error("OCSP exception: ", ex);
        }

        return status;
    }

    private OCSPReq generateOCSPRequest(CertificateID certificateId) throws OCSPException {
        OCSPReqBuilder ocspReqGenerator = new OCSPReqBuilder();

        ocspReqGenerator.addRequest(certificateId);

        return ocspReqGenerator.build();
    }

    @SuppressWarnings({"deprecation", "resource"})
    private String getOCSPUrl(X509Certificate certificate) throws IOException {
        ASN1Primitive obj;
        try {
            obj = getExtensionValue(certificate, Extension.authorityInfoAccess.getId());
        } catch (IOException ex) {
            log.error("Failed to get OCSP URL", ex);
            return null;
        }

        if (obj == null) {
            return null;
        }

        AuthorityInformationAccess authorityInformationAccess = AuthorityInformationAccess.getInstance(obj);

        AccessDescription[] accessDescriptions = authorityInformationAccess.getAccessDescriptions();
        for (AccessDescription accessDescription : accessDescriptions) {
            boolean correctAccessMethod = accessDescription.getAccessMethod().equals(X509ObjectIdentifiers.ocspAccessMethod);
            if (!correctAccessMethod) {
                continue;
            }

            GeneralName name = accessDescription.getAccessLocation();
            if (name.getTagNo() != GeneralName.uniformResourceIdentifier) {
                continue;
            }

            DERIA5String derStr = DERIA5String.getInstance((ASN1TaggedObject) name.toASN1Primitive(), false);
            return derStr.getString();
        }

        return null;

    }

    public OCSPResp requestOCSPResponse(String url, OCSPReq ocspReq) throws IOException {
        byte[] ocspReqData = ocspReq.getEncoded();

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setRequestProperty("Content-Type", "application/ocsp-request");
            con.setRequestProperty("Accept", "application/ocsp-response");

            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);

            OutputStream out = con.getOutputStream();
            try {
                IOUtils.write(ocspReqData, out);
                out.flush();
            } finally {
                IOUtils.closeQuietly(out);
            }

            byte[] responseBytes = IOUtils.toByteArray(con.getInputStream());
            return new OCSPResp(responseBytes);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    @Override
    public void destroy() {
        // nothing to destroy
    }

}
