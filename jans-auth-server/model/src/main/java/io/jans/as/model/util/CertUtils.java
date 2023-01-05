/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.util.encoders.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.AlgorithmParameters;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CertUtils {

    private static final Logger log = LoggerFactory.getLogger(CertUtils.class);

    private CertUtils() {
    }

    public static SignatureAlgorithm getSignatureAlgorithm(X509Certificate cert) {
        String signAlgName = cert.getSigAlgName();

        for (SignatureAlgorithm sa : SignatureAlgorithm.values()) {
            if (signAlgName.equalsIgnoreCase(sa.getAlgorithm())) {
                return sa;
            }
        }

        /*
        Ensures that SignatureAlgorithms `PS256`, `PS384`, and `PS512` work properly on JDK 11 and later without the need
        for BouncyCastle.  Previous releases referenced a BouncyCastle-specific
        algorithm name instead of the Java Security Standard Algorithm Name of
        [`RSASSA-PSS`](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#signature-algorithms).
        This release ensures the standard name is used moving forward.
         */
        if ("RSASSA-PSS".equals(signAlgName)) {
            AlgorithmParameters algorithmParameters = CertUtils.getAlgorithmParameters(cert);
            if (algorithmParameters == null) {
                return null;
            }

            String algParamString = algorithmParameters.toString();
            if (algParamString.contains("SHA-256")) {
                return SignatureAlgorithm.PS256;
            }
            if (algParamString.contains("SHA-384")) {
                return SignatureAlgorithm.PS384;
            }
            if (algParamString.contains("SHA-512")) {
                return SignatureAlgorithm.PS512;
            }
        }
        return null;
    }

    public static AlgorithmParameters getAlgorithmParameters(X509Certificate cert) {
        try {
            AlgorithmParameters result = AlgorithmParameters.getInstance(cert.getSigAlgName());
            result.init(cert.getSigAlgParams());
            return result;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public static X509Certificate x509CertificateFromBytes(byte[] cert) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream bais = new ByteArrayInputStream(cert);

            return (X509Certificate) certFactory.generateCertificate(bais);
        } catch (Exception ex) {
            log.error("Failed to parse X.509 certificates from bytes", ex);
        }

        return null;
    }

    /**
     * @param pem (e.g. "-----BEGIN CERTIFICATE-----MIICsDCCAZigAwIBAgIIdF+Wcca7gzkwDQYJKoZIhvcNAQELBQAwGDEWMBQGA1UEAwwNY2FvajdicjRpcHc2dTAeFw0xNzA4MDcxNDMyMzVaFw0xODA4MDcxNDMyMzZaMBgxFjAUBgNVBAMMDWNhb2o3YnI0aXB3NnUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCdrt40Otrveq46K3BzZuds6wDqsP0kZV+C3GdyTQWl53orBRtPIiEh6BauP17Rr19qadh7t4yFBb5thrXwBewseSNEL4j7sB0YoeNwRsmA29Fjfoe0yeNpLixFadL6dz7ej9xW2suPppIO6jA5SYgL6+S42ZlIauCnSQBKFcdP8QRvgDZBZ4A7CmuloRJst7GQzppa+YWR+Zg3V5reV8Ekrkjxhwgd+rMsGahxijY7Juf2zMgLOXwe68y41SGnn+1RwezAhnJgioGiwY2gP7z2m8yNZXhpUiX+KAP2xvYb60wNYOswuqfpya68rSmYT8mQjld1EPR21dBMjRQ8HfUBAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAAIUlqltRlbqiolGETmAUF8AiC008UCUmI+IsnORbHFSaACKW04m1iFH0OlxuAE1ECj1mlTcKb4md6i7n+Fy+fdGXFL73yhlSiBLu7XW5uN1/dAkynA+mXC5BDFijmvkEAgNLKyh40u/U1u75v2SFS+kLyMeqmVxvUHA7qA8VgyHi/FZzXCfEvxK5jye4L8tkAR34x5j5MpPDMfLkwLegUG+ygX+h/f8luKiQAk7eD4C59c/F0PpigvzcMpyg8+SE9loIEuJ9dRaRaTwIzez3QA7PJtrhu9h0TooTtkmF/Zw9HARrO0qXgT8uNtQDcRXZCItt1Qr7cOJyx2IjTFR2rE=-----END CERTIFICATE-----";)
     * @return x509 certificate
     */
    public static X509Certificate x509CertificateFromPem(String pem) {
        try {
            final X509Certificate result = x509CertificateFromPemInternal(pem);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            log.trace("Failed to parse pem. {}, trying to url decode it.", e.getMessage());
        }
        try {
            return x509CertificateFromPemInternal(URLDecoder.decode(pem, Util.UTF8_STRING_ENCODING));
        } catch (Exception e) {
            log.error("Failed to parse pem", e);
            return null;
        }
    }

    private static X509Certificate x509CertificateFromPemInternal(String pem) {
        pem = StringUtils.remove(pem, "-----BEGIN CERTIFICATE-----");
        pem = StringUtils.remove(pem, "-----END CERTIFICATE-----");
        return x509CertificateFromBytes(Base64.decode(pem));
    }

    public static String confirmationMethodHashS256(String certificateAsPem) {
        if (org.apache.commons.lang.StringUtils.isBlank(certificateAsPem)) {
            return "";
        }
        try {
            return confirmationMethodHashS256Internal(certificateAsPem);
        } catch (Exception e) {
            try {
                return confirmationMethodHashS256Internal(URLDecoder.decode(certificateAsPem, Util.UTF8_STRING_ENCODING));
            } catch (Exception ex) {
                log.error("Failed to hash certificate: " + certificateAsPem, ex);
                return "";
            }
        }
    }

    private static String confirmationMethodHashS256Internal(String certificateAsPem) {
        certificateAsPem = org.apache.commons.lang.StringUtils.remove(certificateAsPem, "-----BEGIN CERTIFICATE-----");
        certificateAsPem = org.apache.commons.lang.StringUtils.remove(certificateAsPem, "-----END CERTIFICATE-----");
        certificateAsPem = StringUtils.replace(certificateAsPem, "\n", "");
        return Base64Util.base64urlencode(DigestUtils.sha256(Base64.decode(certificateAsPem)));
    }

    @NotNull
    public static String getCN(@Nullable X509Certificate cert) {
        return getAttr(cert, BCStyle.CN);
    }

    @NotNull
    public static String getAttr(@Nullable X509Certificate cert, ASN1ObjectIdentifier attrName) {
        try {
            if (cert == null) {
                return "";
            }
            X500Name x500name = new JcaX509CertificateHolder(cert).getSubject();
            final RDN[] rdns = x500name.getRDNs(attrName);
            if (rdns == null || rdns.length == 0) {
                return "";
            }
            RDN cn = rdns[0];

            if (cn != null && cn.getFirst() != null && cn.getFirst().getValue() != null) {
                return IETFUtils.valueToString(cn.getFirst().getValue());
            }
        } catch (CertificateEncodingException e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public static boolean equalsRdn(String rdn1, String rdn2) {
        if (StringUtils.isBlank(rdn1) || StringUtils.isBlank(rdn2)) {
            return false;
        }

        X500Name n1 = new X500Name(BCStyleExtended.INSTANCE, rdn1);
        X500Name n2 = new X500Name(BCStyleExtended.INSTANCE, rdn2);

        return n1.equals(n2);
    }
}
