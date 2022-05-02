/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.common.service.common;

import io.jans.as.model.common.IdType;
import io.jans.as.model.util.Pair;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/**
 * Provides operations with clients.
 *
 * @author Javier Rojas Date: 01.12.2012
 */
@ApplicationScoped
public class InumService {

    public static final int MAX_IDGEN_TRY_COUNT = 10;

    @Inject
    private Logger log;

    @Inject
    private ExternalIdGeneratorService externalIdGenerationService;

    public String generateClientInum() {
        return generateId(IdType.CLIENTS.getType());
    }

    public String generatePeopleInum() {
        return generateId(IdType.PEOPLE.getType());
    }

    public String generateInum() {
        return generateClientInum();
    }

    public Pair<String, String> generateNewDN(String baseDn) {
        final String inum = generateInum();
        final StringBuilder dnSb = new StringBuilder("inum=");
        dnSb.append(inum).append(",").append(baseDn);
        final String dn = dnSb.toString();
        log.trace("Generated dn: {}", dn);
        return new Pair<>(inum, dn);
    }

    public String generateId(String idType) {
        if (externalIdGenerationService.isEnabled()) {
            final String generatedId = externalIdGenerationService.executeExternalDefaultGenerateIdMethod("oxauth", idType, "");

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