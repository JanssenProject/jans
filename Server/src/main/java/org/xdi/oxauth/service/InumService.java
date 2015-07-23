/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.idgen.ws.rs.IdGenService;
import org.xdi.oxauth.model.common.IdType;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.util.Pair;
import org.xdi.oxauth.util.ServerUtil;

/**
 * Provides operations with clients.
 *
 * @author Javier Rojas Date: 01.12.2012
 */
@Scope(ScopeType.STATELESS)
@Name("inumService")
@AutoCreate
public class InumService {

    @Logger
    private Log log;
    @In
    private IdGenService idGenService;

    public String generateClientInum() {
        return generateClientInum(ConfigurationFactory.instance().getConfiguration().getOrganizationInum());
    }

    public String generateClientInum(String p_organizationInum) {
        return idGenService.generateId(IdType.CLIENTS, p_organizationInum);
    }

    public String generatePeopleInum() {
        return idGenService.generateId(IdType.PEOPLE, ConfigurationFactory.instance().getConfiguration().getOrganizationInum());
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

    public static InumService instance() {
        return ServerUtil.instance(InumService.class);
    }
}