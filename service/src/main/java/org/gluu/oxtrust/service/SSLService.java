/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.encoders.Base64;
import org.gluu.oxtrust.util.ServiceUtil;
import org.slf4j.Logger;

/**
 * Provides common ssl certificates management
 * 
 * @author �Oleksiy Tataryn�
 */
@ApplicationScoped
public class SSLService implements Serializable {

	private static final long serialVersionUID = -874807269234589084L;

	@Inject
	private Logger log;

	/** Bouncy Castle SecurityProvider */
	private static final String SECURITY_PROVIDER_BOUNCY_CASTLE = "BC";
	private static final String X509_CERT_TYPE = "X.509";
	private static final String PKI_PATH_ENCODING = "PkiPath";
	private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
	private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

	/**
	 * Extracts X509 certificate from pem-encoded file.
	 * 
	 * @param fileName
	 * @return
	 */
	public X509Certificate getPEMCertificate(String fileName) {
		X509Certificate cert = null;
		try {
			cert = getPEMCertificate(new FileInputStream(fileName));
		} catch (FileNotFoundException e) {
			log.error("Certificate file does not exist : " + fileName);
		}
		return cert;
	}

	/**
	 * Extracts X509 certificate from pem-encoded stream.
	 * 
	 * @param certStream
	 * @return
	 */
	public X509Certificate getPEMCertificate(byte[] cert) {
		try (ByteArrayInputStream bis = new ByteArrayInputStream(cert)) {
			return getPEMCertificate(bis);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Extracts X509 certificate from pem-encoded stream.
	 * 
	 * @param certStream
	 * @return
	 */
	public X509Certificate getPEMCertificate(InputStream certStream) {
		try {
			return getPEMCertificateStatic(certStream);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Extracts X509 certificate from pem-encoded stream.
	 * 
	 * @param certStream
	 * @return
	 */
	public static X509Certificate getPEMCertificateStatic(InputStream certStream) throws Exception {
		try (Reader reader = new InputStreamReader(certStream);PEMParser r = new PEMParser(reader)) {
			Object certObject = r.readObject();

			if (certObject instanceof X509Certificate) {
				return (X509Certificate) certObject;
			} else if (certObject instanceof X509CertificateHolder) {
				X509CertificateHolder certificateHolder = (X509CertificateHolder) certObject;
				return new JcaX509CertificateConverter().setProvider(SECURITY_PROVIDER_BOUNCY_CASTLE)
						.getCertificate(certificateHolder);
			} else {
				throw new IOException("unknown certificate type");
			}
		} catch (Exception e) {
		    throw new IOException("unknown certificate type");
        }
	}

	/**
	 * Load one or more certificates from the specified stream.
	 *
	 * @param is
	 *            Stream to load certificates from
	 * @return The array of certificates
	 */
	public static X509Certificate[] loadCertificates(InputStream is) throws Exception {
		byte[] certsBytes = ServiceUtil.readFully(is);

		return loadCertificates(certsBytes);
	}

	/**
	 * Load one or more certificates from the specified byte array.
	 *
	 * @param certsBytes
	 *            Byte array to load certificates from
	 * @return The array of certificates
	 */
	public static X509Certificate[] loadCertificates(byte[] certsBytes) throws Exception {
		try {
			// fix common input certificate problems by converting PEM/B64 to DER
			certsBytes = fixCommonInputCertProblems(certsBytes);

			CertificateFactory cf = getCertificateFactoryInstance();

			Collection<? extends Certificate> certs = cf.generateCertificates(new ByteArrayInputStream(certsBytes));

			ArrayList<X509Certificate> loadedCerts = new ArrayList<X509Certificate>();

			for (Iterator<? extends Certificate> itr = certs.iterator(); itr.hasNext();) {
				X509Certificate cert = (X509Certificate) itr.next();

				if (cert != null) {
					loadedCerts.add(cert);
				}
			}

			return loadedCerts.toArray(new X509Certificate[loadedCerts.size()]);
		} catch (CertificateException ex) {
			try {
				// Failed to load certificates, may be pki path encoded - try loading as that
				return loadCertificatesAsPkiPathEncoded(new ByteArrayInputStream(certsBytes));
			} catch (CertificateException e) {
				// Failed to load certificates, may be PEM certificate
				X509Certificate certs[] = new X509Certificate[1];
				certs[0] = getPEMCertificateStatic(new ByteArrayInputStream(certsBytes));
				return certs;
			}
		}
	}

	private static X509Certificate[] loadCertificatesAsPkiPathEncoded(InputStream is) throws Exception {
		try {
			CertificateFactory cf = getCertificateFactoryInstance();
			CertPath certPath = cf.generateCertPath(is, PKI_PATH_ENCODING);

			List<? extends Certificate> certs = certPath.getCertificates();

			ArrayList<X509Certificate> loadedCerts = new ArrayList<X509Certificate>();

			for (Iterator<? extends Certificate> itr = certs.iterator(); itr.hasNext();) {
				X509Certificate cert = (X509Certificate) itr.next();

				if (cert != null) {
					loadedCerts.add(cert);
				}
			}

			return loadedCerts.toArray(new X509Certificate[loadedCerts.size()]);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	private static byte[] fixCommonInputCertProblems(byte[] certs) throws IOException {

		// clear PEM header/footer
		String certsStr = new String(certs);
		if (certsStr.startsWith(BEGIN_CERTIFICATE)) {
			certsStr = certsStr.replaceAll(BEGIN_CERTIFICATE, "");
			certsStr = certsStr.replaceAll(END_CERTIFICATE, "");
		}

		// check for base 64 encoded and decode if necessary
		byte[] decoded = attemptBase64Decode(certsStr);
		if (decoded != null) {
			return decoded;
		}

		return certs;
	}

	private static byte[] attemptBase64Decode(String toTest) {
		// Attempt to decode the supplied byte array as a base 64 encoded SPC.
		// Character set may be UTF-16 big endian or ASCII.

		char[] base64 = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
				'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
				'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', '+', '/', '=' };

		// remove all non visible characters (like newlines) and whitespace
		toTest = toTest.replaceAll("\\s", "");

		/*
		 * Check all characters are base 64. Discard any zero bytes that be present if
		 * UTF-16 encoding is used but will mess up a base 64 decode
		 */
		StringBuffer sb = new StringBuffer();

		nextChar: for (int i = 0; i < toTest.length(); i++) {
			char c = toTest.charAt(i);

			for (int j = 0; j < base64.length; j++) {
				if (c == base64[j]) // Append base 64 byte
				{
					sb.append(c);
					continue nextChar;
				} else if (c == 0) // Discard zero byte
				{
					continue nextChar;
				}
			}

			return null; // Not base 64
		}

		return Base64.decode(sb.toString());
	}

	/**
	 * Load a CRL from the specified stream.
	 *
	 * @param is
	 *            Stream to load CRL from
	 * @return The CRL
	 * @throws Exception
	 *             Problem encountered while loading the CRL
	 */
	public static X509CRL loadCRL(InputStream is) throws Exception {
		try {
			CertificateFactory cf = getCertificateFactoryInstance();
			X509CRL crl = (X509CRL) cf.generateCRL(is);
			return crl;
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	/**
	 * Convert the supplied array of certificate objects into X509Certificate
	 * objects.
	 *
	 * @param certsIn
	 *            The Certificate objects
	 * @return The converted X509Certificate objects
	 * @throws Exception
	 *             A problem occurred during the conversion
	 */
	public static X509Certificate[] convertCertificates(Certificate[] certsIn) throws Exception {
		if (certsIn == null) {
			return new X509Certificate[0];
		}

		X509Certificate[] certsOut = new X509Certificate[certsIn.length];

		for (int i = 0; i < certsIn.length; i++) {
			certsOut[i] = convertCertificate(certsIn[i]);
		}

		return certsOut;
	}

	/**
	 * Convert the supplied certificate object into an X509Certificate object.
	 *
	 * @param cert
	 *            The Certificate object
	 * @return The converted X509Certificate object
	 * @throws Exception
	 *             A problem occurred during the conversion
	 */
	public static X509Certificate convertCertificate(Certificate cert) throws Exception {
		CertificateFactory cf = getCertificateFactoryInstance();
		ByteArrayInputStream bais = new ByteArrayInputStream(cert.getEncoded());
		return (X509Certificate) cf.generateCertificate(bais);
	}

	/**
	 * Get BOUNCY CASTLE CertificateFactory instance.
	 * 
	 * @return
	 * @throws CertificateException
	 * @throws NoSuchProviderException
	 */
	public static CertificateFactory getCertificateFactoryInstance()
			throws CertificateException, NoSuchProviderException {
		return CertificateFactory.getInstance(X509_CERT_TYPE, SECURITY_PROVIDER_BOUNCY_CASTLE);
	}
}
