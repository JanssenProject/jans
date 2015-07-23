/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.resourceserver;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.uma.RegisterPermissionRequest;
import org.xdi.oxauth.model.uma.ResourceSetPermissionTicket;
import org.xdi.oxauth.model.uma.persistence.ResourceSet;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.ResourceSetPermissionManager;
import org.xdi.oxauth.uma.ws.rs.PermissionRegistrationWS;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.Pair;

import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/07/2013
 */
@Scope(ScopeType.STATELESS)
@Name("umaRsPermissionService")
@AutoCreate
public class PermissionService {

    @Logger
    private Log log;
    @In
    private RsResourceService umaRsResourceService;
    @In
    private TokenService tokenService;
    @In
    private ResourceSetPermissionManager resourceSetPermissionManager;

    public static PermissionService instance() {
        return ServerUtil.instance(PermissionService.class);
    }

    public Pair<Boolean, Response> hasEnoughPermissionsWithTicketRegistration(UmaRPT p_rpt, List<ResourceSetPermission> p_rptPermissions, RsResourceType p_resourceType, List<RsScopeType> p_scopes) {
        final Pair<Boolean, Response> result = new Pair<Boolean, Response>(false, null);
        final ResourceSet resource = umaRsResourceService.getResource(p_resourceType);
        if (resource == null || StringUtils.isBlank(resource.getId())) {
            result.setFirst(false);
            result.setSecond(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            return result;
        }


        if (hasEnoughPermissions(p_rpt, p_rptPermissions, resource, p_scopes)) {
            result.setFirst(true);
            return result;
        } else {
            // If the RPT is valid but has insufficient authorization data for the type of access sought,
            // the resource server SHOULD register a requested permission with the authorization server
            // that would suffice for that scope of access (see Section 3.2),
            // and then respond with the HTTP 403 (Forbidden) status code,
            // along with providing the authorization server's URI in an "as_uri" property in the header,
            // and the permission ticket it just received from the AM in the body in a JSON-encoded "ticket" property.
            result.setFirst(false);
            final String ticket = registerPermission(p_rpt, resource, p_scopes);
            //                    LOG.debug("Register permissions on AM, permission ticket: " + ticket);

            final String entity = ServerUtil.asJsonSilently(new ResourceSetPermissionTicket(ticket));

            log.debug("Construct response: HTTP 403 (Forbidden), entity: " + entity);
            final Response response = Response.status(Response.Status.FORBIDDEN).
                    header("host_id", ConfigurationFactory.instance().getConfiguration().getIssuer()).
                    header("as_uri", ConfigurationFactory.instance().getConfiguration().getUmaConfigurationEndpoint()).
                    header("error", "insufficient_scope").
                    entity(entity).
                    build();
            result.setSecond(response);
            return result;
        }

    }

    private boolean hasEnoughPermissions(UmaRPT p_rpt, List<ResourceSetPermission> p_rptPermissions, ResourceSet p_resource, List<RsScopeType> p_scopes) {
        if (p_rptPermissions != null && !p_rptPermissions.isEmpty()) {
            final List<String> scopeDns = umaRsResourceService.getScopeDns(p_scopes);
            for (ResourceSetPermission p : p_rptPermissions) {
                if (hasAny(p, scopeDns)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAny(ResourceSetPermission p_p, List<String> p_scopeDns) {
        final List<String> scopeDns = p_p.getScopeDns();
        if (scopeDns != null && !scopeDns.isEmpty() && p_scopeDns != null && !p_scopeDns.isEmpty()) {
            for (String scopeDn : scopeDns) {
                if (p_scopeDns.contains(scopeDn)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String registerPermission(UmaRPT p_rpt, ResourceSet p_resource, List<RsScopeType> p_scopes) {
        final Date expirationDate = PermissionRegistrationWS.rptExpirationDate();

        final RegisterPermissionRequest r = new RegisterPermissionRequest();
        r.setResourceSetId(p_resource.getId());
        r.setExpiresAt(expirationDate);

        final String host = ConfigurationFactory.instance().getConfiguration().getIssuer();
        final ResourceSetPermission permission = resourceSetPermissionManager.createResourceSetPermission(
                host, r, expirationDate);
        // IMPORTANT : set scope dns before persistence
        permission.setScopeDns(umaRsResourceService.getScopeDns(p_scopes));
        final Client client = ClientService.instance().getClient(p_rpt.getClientId());
        resourceSetPermissionManager.addResourceSetPermission(permission, client.getDn());
        return permission.getTicket();
    }
}
