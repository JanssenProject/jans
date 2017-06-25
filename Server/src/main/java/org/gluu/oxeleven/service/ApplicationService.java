/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxeleven.service;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.gluu.oxeleven.model.Configuration;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan Date: 06/25/2017
 */
@ApplicationScoped
public class ApplicationService {

	@Inject
	private Logger log;

	@Inject
	private Configuration configuration;

	@Produces
	@ApplicationScoped
	public PKCS11Service createPKCS11Service() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
		log.info("Creating PKCS11Service service");

		String pkcs11Pin = configuration.getPkcs11Pin();
		Map<String, String> pkcs11Config = configuration.getPkcs11Config();

		PKCS11Service pkcs11Service = new PKCS11Service();
		pkcs11Service.init(pkcs11Pin, pkcs11Config);

		return pkcs11Service;
	}

}
