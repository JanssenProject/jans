/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.crypto.cert;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

import io.jans.util.security.SecurityProviderUtility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateParser {

    public static X509Certificate parsePem(String pemEncodedCert) throws CertificateException {
        StringReader sr = new StringReader(pemEncodedCert);
        PEMParser pemReader = new PEMParser(sr);
        try {
            X509CertificateHolder certificateHolder = ((X509CertificateHolder) pemReader.readObject());
            if (certificateHolder == null) {
                return null;
            }

            return new JcaX509CertificateConverter().setProvider(SecurityProviderUtility.getBCProviderName()).getCertificate(certificateHolder);
        } catch (IOException ex) {
            throw new CertificateException(ex);
        } finally {
            IOUtils.closeQuietly(pemReader);
        }
    }

    public static X509Certificate parseDer(String base64DerEncodedCert) throws CertificateException {
        return parseDer(Base64.decodeBase64(base64DerEncodedCert));
    }

    public static X509Certificate parseDer(byte[] derEncodedCert) throws CertificateException {
        return parseDer(new ByteArrayInputStream(derEncodedCert));
    }

    public static X509Certificate parseDer(InputStream is) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509", SecurityProviderUtility.getBCProvider()).generateCertificate(is);
    }
}
