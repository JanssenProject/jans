package org.xdi.oxauth.service.uma;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Movchan
 * @version 0.9, 14/02/2013
 */

public class RPTManagerInMemory extends AbstractRPTManager implements Serializable {

    private static final long serialVersionUID = -5437567020929600776L;

//    private static final Log LOG = Logging.getLog(RPTManagerInMemory.class);

    private final ConcurrentHashMap<String, UmaRPT> codeToRPT = new ConcurrentHashMap<String, UmaRPT>();
    private final ResourceSetPermissionManager permissionManager;

    public RPTManagerInMemory() {
        permissionManager = ServerUtil.instance(ResourceSetPermissionManager.class);
    }

    public void addRPT(UmaRPT requesterPermissionToken, String clientDn) {
        String requesterPermissionTokenKey = requesterPermissionToken.getCode();

        // Remove old RPT token if needed
        codeToRPT.remove(requesterPermissionTokenKey);

        // Store new RPT token
        codeToRPT.put(requesterPermissionToken.getCode(), requesterPermissionToken);
    }

    public UmaRPT getRPTByCode(String requesterPermissionTokenCode) {
        return codeToRPT.get(requesterPermissionTokenCode);
    }

    public void deleteRPT(String rptCode) {
        this.codeToRPT.remove(rptCode);
    }

    public void cleanupRPTs(Date now) {
        for (Iterator<Map.Entry<String, UmaRPT>> it = this.codeToRPT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, UmaRPT> requesterPermissionTokenEntry = it.next();
            UmaRPT requesterPermissionToken = requesterPermissionTokenEntry.getValue();

            requesterPermissionToken.checkExpired(now);
            if (!requesterPermissionToken.isValid()) {
                it.remove();
            }
        }
    }

    @Override
    public synchronized void addPermissionToRPT(UmaRPT p_rpt, ResourceSetPermission p_permission) {
        final List<String> list = new ArrayList<String>(p_rpt.getPermissions());
        list.add(p_permission.getTicket());
        p_rpt.setPermissions(list);
    }

    @Override
    public List<ResourceSetPermission> getRptPermissions(UmaRPT p_rpt) {
        final List<ResourceSetPermission> result = new ArrayList<ResourceSetPermission>();
        final List<String> permissionTickets = p_rpt.getPermissions();
        if (permissionTickets != null && !permissionTickets.isEmpty()) {
            for (String ticket : permissionTickets) {
                final ResourceSetPermission permission = permissionManager.getResourceSetPermissionByTicket(ticket);
                if (permission != null) {
                    result.add(permission);
                }
            }
        }
        return result;
    }

    @Override
    public ResourceSetPermission getPermissionFromRPTByResourceSetId(UmaRPT p_rpt, String p_resourceSetId) {
        final List<String> permissionTickets = p_rpt.getPermissions();
        if (StringUtils.isNotBlank(p_resourceSetId) && permissionTickets != null && !permissionTickets.isEmpty()) {
            for (String ticket : permissionTickets) {
                final ResourceSetPermission permission = permissionManager.getResourceSetPermissionByTicket(ticket);
                if (permission != null && p_resourceSetId.equals(permission.getResourceSetId())) {
                    return permission;
                }
            }
        }
        return null;
    }
}
