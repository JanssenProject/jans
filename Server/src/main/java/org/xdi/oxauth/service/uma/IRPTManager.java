package org.xdi.oxauth.service.uma;

import org.xdi.oxauth.model.common.AbstractToken;
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

    public UmaRPT createRPT(AbstractToken authorizationApiToken, String userId, String clientId, String amHost);

    public void deleteRPT(String rptCode);

    public void cleanupRPTs(Date now);

    public void addPermissionToRPT(UmaRPT p_rpt, ResourceSetPermission p_permission);

    public ResourceSetPermission getPermissionFromRPTByResourceSetId(UmaRPT p_rpt, String p_resourceSetId);

    public List<ResourceSetPermission> getRptPermissions(UmaRPT p_rpt);
}
