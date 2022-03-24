/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.fido2.model.cert.CertificateHolder;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class KeyStoreCreator {

	@Inject
	private Logger log;

	@Inject
	private Base64Service base64Service;

	public KeyStore createKeyStore(List<CertificateHolder> certificates) {
		byte[] password = new byte[200];
		new SecureRandom().nextBytes(password);

		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, base64Service.encodeToString(password).toCharArray());

			certificates.stream().forEach(ch -> {
				try {
					ks.setCertificateEntry(ch.getAlias(), ch.getCert());
				} catch (KeyStoreException e) {
					log.warn("Can't load certificate {} {}", ch.getAlias(), e.getMessage());
				}
			});
			return ks;
		} catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
			throw new RuntimeException(e);
		}
	}

	public KeyStore createKeyStore(String aaguid, List<X509Certificate> certificates) {
		byte[] password = new byte[200];
		new SecureRandom().nextBytes(password);

		try {
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(null, base64Service.encodeToString(password).toCharArray());

			AtomicInteger counter = new AtomicInteger(0);

			certificates.stream().forEach(ch -> {
				String alias = aaguid + "-" + counter.incrementAndGet();
				try {
					ks.setCertificateEntry(alias, ch);
				} catch (KeyStoreException e) {
					log.warn("Can't load certificate {} {}", alias, e.getMessage());
				}
			});

			return ks;
		} catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
			log.error("Failed to creae KeyStore", ex);
			throw new RuntimeException(ex);
		}
	}

}
