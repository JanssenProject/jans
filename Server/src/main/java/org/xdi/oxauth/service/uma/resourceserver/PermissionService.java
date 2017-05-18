/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.resourceserver;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.uma.persistence.UmaResource;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.token.TokenService;
import org.xdi.oxauth.service.uma.UmaPermissionManager;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.Pair;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/07/2013
 */
@Stateless
@Named("umaRsPermissionService")
public class PermissionService {

    public static final int DEFAULT_PERMISSION_LIFETIME = 3600;

    @Inject
    private Logger log;

    @Inject
    private RsResourceService umaRsResourceService;

    @Inject
    private TokenService tokenService;

    @Inject
    private UmaPermissionManager resourceSetPermissionManager;

    @Inject
    private AppConfiguration appConfiguration;
    
    @Inject
    private ClientService clientService;

    public Pair<Boolean, Response> hasEnoughPermissionsWithTicketRegistration(UmaRPT p_rpt, List<UmaPermission> p_rptPermissions, RsResourceType p_resourceType, List<RsScopeType> p_scopes) {
        final Pair<Boolean, Response> result = new Pair<Boolean, Response>(false, null);
        final UmaResource resource = umaRsResourceService.getResource(p_resourceType);
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
            //                    log.debug("Register permissions on AM, permission ticket: " + ticket);

            final String entity = ServerUtil.asJsonSilently(new PermissionTicket(ticket));

            log.debug("Construct response: HTTP 403 (Forbidden), entity: " + entity);
            final Response response = Response.status(Response.Status.FORBIDDEN).
                    header("host_id", appConfiguration.getIssuer()).
                    header("as_uri", appConfiguration.getUmaConfigurationEndpoint()).
                    header("error", "insufficient_scope").
                    entity(entity).
                    build();
            result.setSecond(response);
            return result;
        }

    }

    private boolean hasEnoughPermissions(UmaRPT p_rpt, List<UmaPermission> p_rptPermissions, UmaResource p_resource, List<RsScopeType> p_scopes) {
        if (p_rptPermissions != null && !p_rptPermissions.isEmpty()) {
            final List<String> scopeDns = umaRsResourceService.getScopeDns(p_scopes);
            for (UmaPermission p : p_rptPermissions) {
                if (hasAny(p, scopeDns)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAny(UmaPermission p_p, List<String> p_scopeDns) {
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

    public Date rptExpirationDate() {
        int lifeTime = appConfiguration.getUmaRequesterPermissionTokenLifetime();
        if (lifeTime <= 0) {
            lifeTime = DEFAULT_PERMISSION_LIFETIME;
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, lifeTime);
        return calendar.getTime();
    }

    private String registerPermission(UmaRPT p_rpt, UmaResource p_resource, List<RsScopeType> p_scopes) {
        final Date expirationDate = rptExpirationDate();

        final org.xdi.oxauth.model.uma.UmaPermission r = new org.xdi.oxauth.model.uma.UmaPermission();
        r.setResourceId(p_resource.getId());
        r.setExpiresAt(expirationDate);

        final String host = appConfiguration.getIssuer();
        final UmaPermission permission = resourceSetPermissionManager.createPermission(
                host, r, expirationDate);
        // IMPORTANT : set scope dns before persistence
        permission.setScopeDns(umaRsResourceService.getScopeDns(p_scopes));
        final Client client = clientService.getClient(p_rpt.getClientId());
        resourceSetPermissionManager.addPermission(permission, client.getDn());
        return permission.getTicket();
    }
}
