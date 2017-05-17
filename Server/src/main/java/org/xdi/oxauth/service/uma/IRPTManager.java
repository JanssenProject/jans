/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;

import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/02/2013
 */

public interface IRPTManager {

    void addRPT(UmaRPT requesterPermissionToken, String clientDn);

    UmaRPT getRPTByCode(String requesterPermissionTokenCode);

    UmaRPT createRPT(IAuthorizationGrant grant, String amHost, String aat);

    void deleteRPT(String rptCode);

    void cleanupRPTs(Date now);

    void addPermissionToRPT(UmaRPT p_rpt, UmaPermission p_permission);

    UmaPermission getPermissionFromRPTByResourceSetId(UmaRPT p_rpt, String p_resourceSetId);

    List<UmaPermission> getRptPermissions(UmaRPT p_rpt);

    UmaRPT createRPT(String authorization, String amHost);
}
