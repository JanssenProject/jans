/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.gluu.oxauth.model.common.IdType;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.util.Pair;
import org.slf4j.Logger;
import org.gluu.oxauth.service.api.IdGenerator;

/**
 * Provides operations with clients.
 *
 * @author Javier Rojas Date: 01.12.2012
 */
@ApplicationScoped
public class InumService {

    @Inject
    private Logger log;

    @Inject @Any
    private IdGenerator idGenService;

    public String generateClientInum() {
        return UUID.randomUUID().toString();
    }

    public String generatePeopleInum() {
        return idGenService.generateId(IdType.PEOPLE, UUID.randomUUID().toString());
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
        return new Pair<String, String>(inum, dn);
    }

}