/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import javax.enterprise.context.ApplicationScoped;
import org.jboss.seam.annotations.AutoCreate;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import javax.inject.Named;
import org.jboss.seam.annotations.Scope;

import org.xdi.oxauth.idgen.ws.rs.IdGenService;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.util.Pair;
import org.xdi.oxauth.util.ServerUtil;

/**
 * Provides operations with clients.
 *
 * @author Javier Rojas Date: 01.12.2012
 */
@Stateless
@Named("inumService")
@AutoCreate
public class InumService {

    @Inject
    private Logger log;
    @Inject
    private IdGenService idGenService;

    @Inject
    private AppConfiguration appConfiguration;

    public String generateClientInum() {
        return generateClientInum(appConfiguration.getOrganizationInum());
    }

    public String generateClientInum(String p_organizationInum) {
        return idGenService.generateId(IdType.CLIENTS, p_organizationInum);
    }

    public String generatePeopleInum() {
        return idGenService.generateId(IdType.PEOPLE, appConfiguration.getOrganizationInum());
    }

    public String generateInum() {
        return generateClientInum();
    }

    public Pair<String, String> generateNewDN(String baseDn) {
        final String inum = generateInum();
        final StringBuilder dnSb = new StringBuilder("inum=");
        dnSb.append(inum).append(",").append(baseDn);
        final String dn = dnSb.toString();
        log.trace("Generated dn: {0}", dn);
        return new Pair<String, String>(inum, dn);
    }

}