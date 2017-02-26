/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.crypto.cert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;

public class CertificateParser {

	public static X509Certificate parsePem(String pemEncodedCert) throws CertificateException {
		StringReader sr = new StringReader(pemEncodedCert);
		PEMParser pemReader = new PEMParser(sr);
		try {
			X509CertificateHolder certificateHolder = ((X509CertificateHolder) pemReader.readObject());
			if (certificateHolder == null) {
				return null;
			}

			X509Certificate cert = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateHolder);

			return cert;
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
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509", "BC").generateCertificate(is);
        } catch (NoSuchProviderException ex) {
            throw new CertificateException(ex);
        }
    }
}
