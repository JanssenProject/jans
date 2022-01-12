/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store;

import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.provider.DocumentStoreProvider;
import io.jans.service.document.store.provider.JcaDocumentStoreProvider;
import io.jans.service.document.store.provider.LocalDocumentStoreProvider;
import io.jans.service.document.store.provider.WebDavDocumentStoreProvider;
import io.jans.util.security.StringEncrypter;
import io.jans.service.document.store.conf.DocumentStoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
public class StandaloneDocumentStoreProviderFactory {

	private static final Logger LOG = LoggerFactory.getLogger(StandaloneDocumentStoreProviderFactory.class);

	private StringEncrypter stringEncrypter;

	public StandaloneDocumentStoreProviderFactory(StringEncrypter stringEncrypter) {
		this.stringEncrypter = stringEncrypter;
	}

	public DocumentStoreProvider getDocumentStoreProvider(DocumentStoreConfiguration documentStoreConfiguration) {
		DocumentStoreType documentStoreType = documentStoreConfiguration.getDocumentStoreType();

		if (documentStoreType == null) {
			LOG.error("Failed to initialize documentStoreProvider, documentStoreProviderType is null. Fallback to LOCAL type.");
			documentStoreType = DocumentStoreType.LOCAL;
		}

		// Create bean
		DocumentStoreProvider documentStoreProvider = null;
		switch (documentStoreType) {
		case LOCAL:
			LocalDocumentStoreProvider localDocumentStoreProvider = new LocalDocumentStoreProvider();
			localDocumentStoreProvider.configure(documentStoreConfiguration);
			localDocumentStoreProvider.init();

			documentStoreProvider = localDocumentStoreProvider;
			break;
		case JCA:
			if (stringEncrypter == null) {
				throw new RuntimeException("Factory is not initialized properly. stringEncrypter is not specified");
			}

			JcaDocumentStoreProvider jcaDocumentStoreProvider = new JcaDocumentStoreProvider();
			jcaDocumentStoreProvider.configure(documentStoreConfiguration, stringEncrypter);
			jcaDocumentStoreProvider.init();

			documentStoreProvider = jcaDocumentStoreProvider;
			break;
		case WEB_DAV:
			if (stringEncrypter == null) {
				throw new RuntimeException("Factory is not initialized properly. stringEncrypter is not specified");
			}

			WebDavDocumentStoreProvider webDavDocumentStoreProvider = new WebDavDocumentStoreProvider();
			webDavDocumentStoreProvider.configure(documentStoreConfiguration);
			webDavDocumentStoreProvider.init();

			documentStoreProvider = webDavDocumentStoreProvider;
			break;
		}

		if (documentStoreProvider == null) {
			throw new RuntimeException(
					"Failed to initialize documentStoreProvider, documentStoreProviderType is unsupported: " + documentStoreType);
		}

		documentStoreProvider.create();

		return documentStoreProvider;
	}

}
