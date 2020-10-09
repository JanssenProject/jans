/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.scim.service;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.util.StringHelper;

import io.jans.scim.service.external.ExternalIdGeneratorService;

/**
 * @author Yuriy Movchan
 * @version 0.1, 01/16/2015
 */

@ApplicationScoped
public class IdGenService {

	public static final int MAX_IDGEN_TRY_COUNT = 10;

	@Inject
	private ExternalIdGeneratorService externalIdGenerationService;

	public String generateId(String idType) {

		if (externalIdGenerationService.isEnabled()) {
			final String generatedId = externalIdGenerationService.executeExternalDefaultGenerateIdMethod("oxtrust",
					idType, "");

			if (StringHelper.isNotEmpty(generatedId)) {
				return generatedId;
			}
		}

		return generateDefaultId();
	}

	public String generateDefaultId() {

		return UUID.randomUUID().toString();
	}
}
