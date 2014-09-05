/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import java.util.Date;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.AbstractToken;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.util.ServerUtil;

/**
 * RPT manager component
 *
 * @author Yuriy Zabrovarnyy Date: 10/16/2012
 */
@AutoCreate
@Scope(ScopeType.APPLICATION)
@Name("rptManager")
public class RPTManager implements IRPTManager {

    @Logger
    private Log log;

    private IRPTManager manager;

    @Create
    public void init() {
        switch (ConfigurationFactory.getConfiguration().getModeEnum()) {
            case IN_MEMORY:
                manager = new RPTManagerInMemory();
                log.info("Created IN-MEMORY UMA RPT manager");
                break;
            case LDAP:
                manager = new RPTManagerLdap();
                log.info("Created LDAP UMA RPT manager");
                break;
            default:
                log.error("Unable to identify mode of the server. (Please check configuration.)");
                throw new IllegalArgumentException("Unable to identify mode of the server. (Please check configuration.)");
        }
    }

    public void addRPT(UmaRPT requesterPermissionToken, String clientDn) {
        manager.addRPT(requesterPermissionToken, clientDn);
    }

    public UmaRPT getRPTByCode(String requesterPermissionTokenCode) {
        return manager.getRPTByCode(requesterPermissionTokenCode);
    }

    public UmaRPT createRPT(AbstractToken authorizationApiToken, String userId, String clientId, String amHost) {
        return manager.createRPT(authorizationApiToken, userId, clientId, amHost);
    }

    public void deleteRPT(String rptCode) {
        manager.deleteRPT(rptCode);
    }

    public void cleanupRPTs(Date now) {
        manager.cleanupRPTs(now);
    }

    @Override
    public void addPermissionToRPT(UmaRPT p_rpt, ResourceSetPermission p_permission) {
        manager.addPermissionToRPT(p_rpt, p_permission);
    }

    @Override
    public ResourceSetPermission getPermissionFromRPTByResourceSetId(UmaRPT p_rpt, String p_resourceSetId) {
        return manager.getPermissionFromRPTByResourceSetId(p_rpt, p_resourceSetId);
    }

    @Override
    public List<ResourceSetPermission> getRptPermissions(UmaRPT p_rpt) {
        return manager.getRptPermissions(p_rpt);
    }

    /**
     * Get RequesterPermissionTokenManager instance
     *
     * @return RequesterPermissionTokenManager instance
     */
    public static RPTManager instance() {
        return ServerUtil.instance(RPTManager.class);
    }

}
