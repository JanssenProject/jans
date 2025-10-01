package io.jans.orm.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Utility methods to help work with certs
 *
 * @author Yuriy Movchan Date: 07/07/2021
 */
public class CertUtils {

	public static boolean isFips() {
		java.security.Provider[] providers = java.security.Security.getProviders();
		for (int i = 0; i < providers.length; i++) {
			if (providers[i].getName().toLowerCase().contains("fips"))
				return true;
		}

		return false;
	}

	public static TrustManager[] getTrustManagers(String trustStoreFile, String trustStorePin, String trustStoreType)
			throws CertificateException {
		String useTrustStoreType = trustStoreType;
		if (useTrustStoreType == null) {
			useTrustStoreType = KeyStore.getDefaultType();
		}

		char[] useTrustStorePin = null;
		if (StringHelper.isNotEmpty(trustStorePin)) {
			useTrustStorePin = trustStorePin.toCharArray();
		}

		KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance(useTrustStoreType);
			if ("pkcs11".equalsIgnoreCase(useTrustStoreType)) {
				keyStore.load(null, useTrustStorePin);
			} else {
				final File f = new File(trustStoreFile);
				if (!f.exists()) {
					throw new CertificateException(String.format("Trustore file '%s' is not exists", trustStoreFile));
				}
				try (FileInputStream inputStream = new FileInputStream(f)) {
					keyStore.load(inputStream, useTrustStorePin);
				}
			}
		} catch (Exception ex) {
			throw new CertificateException("Failed to load truststore", ex);
		}

		try {
			String trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm);
			trustManagerFactory.init(keyStore);
			TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

			return trustManagers;
		} catch (Exception ex) {
			throw new CertificateException("Failed to prepare truststore", ex);
		}
	}

}
