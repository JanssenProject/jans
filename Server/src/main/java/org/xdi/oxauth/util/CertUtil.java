/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.crypto.cert.CertificateParser;
import org.xdi.util.StringHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * @author Yuriy Movchan
 * @version March 11, 2016
 */
public class CertUtil {

    private final static Logger log = LoggerFactory.getLogger(CertUtil.class);

	private CertUtil() {}

	@SuppressWarnings("unchecked")
	public static List<X509Certificate> loadX509CertificateFromFile(String filePath) {
		if (StringHelper.isEmpty(filePath)) {
			log.error("X509Certificate file path is empty");
			
			return null;
		}

		InputStream is;
		try {
			is = FileUtils.openInputStream(new File(filePath));
		} catch (IOException ex) {
			log.error("Failed to read X.509 certificates from file: '" + filePath + "'", ex);
			return null;
		}

		List<X509Certificate> certificates = null;
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			certificates = (List<X509Certificate>) cf.generateCertificates(is);
		} catch (CertificateException ex) {
			log.error("Failed to parse X.509 certificates from file: '" + filePath + "'", ex);
		} finally {
			IOUtils.closeQuietly(is);
		}

		return certificates;
	}

	public static X509Certificate parsePem(String pem) {
		try {
			return CertificateParser.parsePem(pem);
		} catch (CertificateException ex) {
			log.error("Failed to parse PEM certificate", ex);
		}
		
		return null;
	}
}
