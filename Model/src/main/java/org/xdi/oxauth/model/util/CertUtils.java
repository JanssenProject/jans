package org.xdi.oxauth.model.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CertUtils {

    private final static Logger log = LoggerFactory.getLogger(CertUtils.class);

    private CertUtils() {
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
     *
     * @param pem (e.g. "-----BEGIN CERTIFICATE-----MIICsDCCAZigAwIBAgIIdF+Wcca7gzkwDQYJKoZIhvcNAQELBQAwGDEWMBQGA1UEAwwNY2FvajdicjRpcHc2dTAeFw0xNzA4MDcxNDMyMzVaFw0xODA4MDcxNDMyMzZaMBgxFjAUBgNVBAMMDWNhb2o3YnI0aXB3NnUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCdrt40Otrveq46K3BzZuds6wDqsP0kZV+C3GdyTQWl53orBRtPIiEh6BauP17Rr19qadh7t4yFBb5thrXwBewseSNEL4j7sB0YoeNwRsmA29Fjfoe0yeNpLixFadL6dz7ej9xW2suPppIO6jA5SYgL6+S42ZlIauCnSQBKFcdP8QRvgDZBZ4A7CmuloRJst7GQzppa+YWR+Zg3V5reV8Ekrkjxhwgd+rMsGahxijY7Juf2zMgLOXwe68y41SGnn+1RwezAhnJgioGiwY2gP7z2m8yNZXhpUiX+KAP2xvYb60wNYOswuqfpya68rSmYT8mQjld1EPR21dBMjRQ8HfUBAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAAIUlqltRlbqiolGETmAUF8AiC008UCUmI+IsnORbHFSaACKW04m1iFH0OlxuAE1ECj1mlTcKb4md6i7n+Fy+fdGXFL73yhlSiBLu7XW5uN1/dAkynA+mXC5BDFijmvkEAgNLKyh40u/U1u75v2SFS+kLyMeqmVxvUHA7qA8VgyHi/FZzXCfEvxK5jye4L8tkAR34x5j5MpPDMfLkwLegUG+ygX+h/f8luKiQAk7eD4C59c/F0PpigvzcMpyg8+SE9loIEuJ9dRaRaTwIzez3QA7PJtrhu9h0TooTtkmF/Zw9HARrO0qXgT8uNtQDcRXZCItt1Qr7cOJyx2IjTFR2rE=-----END CERTIFICATE-----";)
     * @return x509 certificate
     */
    public static X509Certificate x509CertificateFromPem(String pem) {
        pem = StringUtils.remove(pem, "-----BEGIN CERTIFICATE-----");
        pem = StringUtils.remove(pem, "-----END CERTIFICATE-----");
        return x509CertificateFromBytes(Base64.decode(pem));
    }

    public static String confirmationMethodHashS256(String certificateAsPem) {
        if (org.apache.commons.lang.StringUtils.isBlank(certificateAsPem)) {
            return "";
        }
        certificateAsPem = org.apache.commons.lang.StringUtils.remove(certificateAsPem, "-----BEGIN CERTIFICATE-----");
        certificateAsPem = org.apache.commons.lang.StringUtils.remove(certificateAsPem, "-----END CERTIFICATE-----");
        certificateAsPem = StringUtils.replace(certificateAsPem, "\n", "");
        return Base64Util.base64urlencode(DigestUtils.sha256(Base64.decode(certificateAsPem)));
    }
}
