/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;

import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/02/2013
 */

public interface IRPTManager {

    public void addRPT(UmaRPT requesterPermissionToken, String clientDn);

    public UmaRPT getRPTByCode(String requesterPermissionTokenCode);

    UmaRPT createRPT(IAuthorizationGrant grant, String amHost, String aat, boolean isGat);

    public void deleteRPT(String rptCode);

    public void cleanupRPTs(Date now);

    public void addPermissionToRPT(UmaRPT p_rpt, ResourceSetPermission p_permission);

    public ResourceSetPermission getPermissionFromRPTByResourceSetId(UmaRPT p_rpt, String p_resourceSetId);

    public List<ResourceSetPermission> getRptPermissions(UmaRPT p_rpt);

    UmaRPT createRPT(String authorization, String amHost, boolean isGat);
}
