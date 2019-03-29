/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.security;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.codec.binary.Base64;

/**
 * Helps to load X509Certificate
 *
 * @author Yuriy Movchan Date: 04/24/2014
 */
public final class CertificateHelper {

    private CertificateHelper() { }

    public static X509Certificate loadCertificate(String certificate) throws CertificateException {
        return loadCertificate(certificate.getBytes());
    }

    public static X509Certificate loadCertificate(byte[] certificate) throws CertificateException {
        CertificateFactory fty = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(certificate));
        X509Certificate x509Certificate = (X509Certificate) fty.generateCertificate(bais);

        return x509Certificate;
    }
}
