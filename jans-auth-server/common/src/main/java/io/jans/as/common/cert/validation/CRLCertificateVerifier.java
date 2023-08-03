/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.cert.validation;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.jans.as.common.cert.validation.model.ValidationStatus;
import io.jans.util.security.SecurityProviderUtility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Certificate verifier based on CRL
 *
 * @author Yuriy Movchan
 * @version March 10, 2016
 */
public class CRLCertificateVerifier implements CertificateVerifier {

    private static final Logger log = LoggerFactory.getLogger(CRLCertificateVerifier.class);

    private final int maxCrlSize;

    private final LoadingCache<String, X509CRL> crlCache;

    public CRLCertificateVerifier(final int maxCrlSize) {
        SecurityProviderUtility.installBCProvider(true);

        this.maxCrlSize = maxCrlSize;

        CacheLoader<String, X509CRL> checkedLoader = new CacheLoader<String, X509CRL>() {
            public X509CRL load(String crlURL) throws CertificateException, CRLException, NoSuchProviderException, IOException, ExecutionException {
                X509CRL result = requestCRL(crlURL);
                Preconditions.checkNotNull(result);

                return result;
            }
        };

        this.crlCache = CacheBuilder.newBuilder().maximumSize(10).expireAfterWrite(60, TimeUnit.MINUTES).build(checkedLoader);
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
        ValidationStatus status = new ValidationStatus(certificate, issuer, validationDate, ValidationStatus.ValidatorSourceType.CRL, ValidationStatus.CertificateValidity.UNKNOWN);

        try {
            Principal subjectX500Principal = certificate.getSubjectX500Principal();

            String crlURL = getCrlUri(certificate);
            if (crlURL == null) {
                log.error("CRL's URL for '" + subjectX500Principal + "' is empty");
                return status;
            }

            log.debug("CRL's URL for '" + subjectX500Principal + "' is '" + crlURL + "'");

            X509CRL x509crl = getCrl(crlURL);
            if (!validateCRL(x509crl, certificate, issuer, validationDate)) {
                log.error("The CRL is not valid!");
                status.setValidity(ValidationStatus.CertificateValidity.INVALID);
                return status;
            }

            X509CRLEntry crlEntry = x509crl.getRevokedCertificate(certificate.getSerialNumber());
            if (crlEntry == null) {
                log.debug("CRL status is valid for '" + subjectX500Principal + "'");
                status.setValidity(ValidationStatus.CertificateValidity.VALID);
            } else if (crlEntry.getRevocationDate().after(validationDate)) {
                log.warn("CRL revocation time after the validation date, the certificate '" + subjectX500Principal + "' was valid at " + validationDate);
                status.setRevocationObjectIssuingTime(x509crl.getThisUpdate());
                status.setValidity(ValidationStatus.CertificateValidity.VALID);
            } else {
                log.info("CRL for certificate '" + subjectX500Principal + "' is revoked since " + crlEntry.getRevocationDate());
                status.setRevocationObjectIssuingTime(x509crl.getThisUpdate());
                status.setRevocationDate(crlEntry.getRevocationDate());
                status.setValidity(ValidationStatus.CertificateValidity.REVOKED);
            }
        } catch (Exception ex) {
            log.error("CRL exception: ", ex);
        }

        return status;
    }

    private boolean validateCRL(X509CRL x509crl, X509Certificate certificate, X509Certificate issuerCertificate, Date validationDate) {
        Principal subjectX500Principal = certificate.getSubjectX500Principal();

        if (x509crl == null) {
            log.error("No CRL found for certificate '" + subjectX500Principal + "'");
            return false;
        }

        if (log.isTraceEnabled()) {
            try {
                log.trace("CRL number: " + getCrlNumber(x509crl));
            } catch (IOException ex) {
                log.error("Failed to get CRL number", ex);
            }
        }

        if (!x509crl.getIssuerX500Principal().equals(issuerCertificate.getSubjectX500Principal())) {
            log.error("The CRL must be signed by the issuer '" + subjectX500Principal + "' but instead is signed by '"
                    + x509crl.getIssuerX500Principal() + "'");
            return false;
        }

        try {
            x509crl.verify(issuerCertificate.getPublicKey());
        } catch (Exception ex) {
            log.error("The signature verification for CRL cannot be performed", ex);
            return false;
        }

        log.debug("CRL validationDate: " + validationDate);
        log.debug("CRL nextUpdate: " + x509crl.getThisUpdate());
        log.debug("CRL thisUpdate: " + x509crl.getNextUpdate());

        if (x509crl.getNextUpdate() != null && validationDate.after(x509crl.getNextUpdate())) {
            log.error("CRL is too old");
            return false;
        }

        if (issuerCertificate.getKeyUsage() == null) {
            log.error("There is no KeyUsage extension for certificate '" + subjectX500Principal + "'");
            return false;
        }

        if (!issuerCertificate.getKeyUsage()[6]) {
            log.error("cRLSign bit is not set for CRL certificate'" + subjectX500Principal + "'");
            return false;
        }

        return true;

    }

    private X509CRL getCrl(String url) throws ExecutionException {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            log.error("It's possible to download CRL via HTTP and HTTPS only");
            return null;
        }

        String cacheKey = url.toLowerCase();
        return crlCache.get(cacheKey);
    }

    public X509CRL requestCRL(String url) throws IOException, CertificateException, CRLException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setUseCaches(false);

            InputStream in = new BoundedInputStream(con.getInputStream(), maxCrlSize);
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                X509CRL crl = (X509CRL) certificateFactory.generateCRL(in);
                log.debug("CRL size: " + crl.getEncoded().length + " bytes");

                return crl;
            } finally {
                IOUtils.closeQuietly(in);
            }
        } catch (IOException ex) {
            log.error("Failed to download CRL from '" + url + "'", ex);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return null;
    }

    @SuppressWarnings({"deprecation", "resource"})
    private BigInteger getCrlNumber(X509CRL crl) throws IOException {
        byte[] crlNumberExtensionValue = crl.getExtensionValue(Extension.cRLNumber.getId());
        if (crlNumberExtensionValue == null) {
            return null;
        }

        ASN1OctetString octetString = (ASN1OctetString) (new ASN1InputStream(new ByteArrayInputStream(crlNumberExtensionValue)).readObject());
        byte[] octets = octetString.getOctets();
        ASN1Integer integer = (ASN1Integer) new ASN1InputStream(octets).readObject();
        return integer.getPositiveValue();
    }

    public String getCrlUri(X509Certificate certificate) throws IOException {
        ASN1Primitive obj;
        try {
            obj = getExtensionValue(certificate, Extension.cRLDistributionPoints.getId());
        } catch (IOException ex) {
            log.error("Failed to get CRL URL", ex);
            return null;
        }

        if (obj == null) {
            return null;
        }

        CRLDistPoint distPoint = CRLDistPoint.getInstance(obj);

        DistributionPoint[] distributionPoints = distPoint.getDistributionPoints();
        for (DistributionPoint distributionPoint : distributionPoints) {
            DistributionPointName distributionPointName = distributionPoint.getDistributionPoint();
            if (DistributionPointName.FULL_NAME != distributionPointName.getType()) {
                continue;
            }

            GeneralNames generalNames = (GeneralNames) distributionPointName.getName();
            GeneralName[] names = generalNames.getNames();
            for (GeneralName name : names) {
                if (name.getTagNo() != GeneralName.uniformResourceIdentifier) {
                    continue;
                }

                DERIA5String derStr = DERIA5String.getInstance((ASN1TaggedObject) name.toASN1Primitive(), false);
                return derStr.getString();
            }
        }

        return null;
    }

    @Override
    public void destroy() {
        crlCache.cleanUp();
    }

}
