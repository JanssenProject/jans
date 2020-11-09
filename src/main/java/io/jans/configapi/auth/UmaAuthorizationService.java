/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.model.uma.wrapper.Token;
import io.jans.configapi.auth.service.PatService;
import io.jans.configapi.auth.service.UmaService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

@ApplicationScoped
@Named("umaAuthorizationService")
public class UmaAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger log;

    @Inject
    UmaService umaService;

    @Inject
    PatService patService;

    public void validateAuthorization(String rpt, ResourceInfo resourceInfo, String method, String path)
            throws Exception {
        log.debug(" UmaAuthorizationService::validateAuthorization() - rpt = " + rpt
                + " , resourceInfo.getClass().getName() = " + resourceInfo.getClass().getName() + " , method = "
                + method + " , path = " + path + "\n");

        UmaResource umaResource = getUmaResource(resourceInfo, method, path);
        log.debug(" UmaAuthorizationService::validateAuthorization() - umaResource = " + umaResource);

        if (umaResource.getScopes() == null || umaResource.getScopes().isEmpty())
            return; // nothing to validate. Resource is not protected.

        validateRptToken(rpt, umaResource);
    }

    public void validateRptToken(String rpt, UmaResource umaResource) throws Exception {

        // Generate PAT token
        Token patToken = patService.getPatToken();
        
        // Validate Token
        umaService.validateRptToken(patToken, rpt, umaResource.getId(), umaResource.getScopes());

    }

    // ??todo: Puja -> To be reviewed by Yuriy Z
    // Reason for this method :: is mismatch in format of UmaResource name compared
    // to the method and path coming from filter
    // Exmaple UMAResource name = [POST, PUT]:::/api/v1/scopes however coming from
    // filter is simply PUT /api/v1/scopes=UmaResource
    // Note that to avoid creating multiple UMAResources with same detail except
    // http method we are combining the http methods.
    // That is rather than creating two UMAResources as, PUT /api/v1/scopes and POST
    // /api/v1/scopes we are creating one UmaResource as [POST,
    // PUT]:::/api/v1/scopes
    // Please let me know if there is a better approach than this?
    private UmaResource getUmaResource(ResourceInfo resourceInfo, String method, String path) {
        log.debug(" UmaAuthorizationService::getUmaResource() - resourceInfo = " + resourceInfo
                + " , resourceInfo.getClass().getName() = " + resourceInfo.getClass().getName() + " , method = "
                + method + " , path = " + path + "\n");
        log.debug(" UmaAuthorizationService::getUmaResource() - umaResourceProtectionCache.getAllUmaResources() = "
                +UmaResourceProtectionCache.getAllUmaResources());

        // Verify in cache
        Map<String, UmaResource> resources = UmaResourceProtectionCache.getAllUmaResources();

        // Filter paths based on resource name
        Set<String> keys = resources.keySet();
        List<String> filteredPaths = keys.stream().filter(k -> k.contains(path)).collect(Collectors.toList());

        log.debug(" UmaAuthorizationService::getUmaResource() - filteredPaths = " + filteredPaths);
        if (filteredPaths == null || filteredPaths.isEmpty()) {
            throw new WebApplicationException("No matching resource found .",
                    Response.status(Response.Status.UNAUTHORIZED).build());
        }

        UmaResource umaResource = null;
        for (String key : filteredPaths) {
            String[] result = key.split(":::");
            if (result != null && result.length > 1) {
                String httpmethod = result[0];
                String pathUrl = result[1];
                log.debug(" UmaAuthorizationService::getUmaResource() - httpmethod = " + httpmethod + " , pathUrl = "
                        + pathUrl);
                if (path.equals(pathUrl)) {
                    // Matching url
                    log.debug(" UmaAuthorizationService::getUmaResource() - Matching url, path = " + path
                            + " , pathUrl = " + pathUrl);

                    // Verify Method
                    if (httpmethod.contains(method)) {
                        umaResource = UmaResourceProtectionCache.getUmaResource(key);
                        log.debug(" UmaAuthorizationService::getUmaResource() - Matching umaResource =" + umaResource);
                        break;
                    }

                }

            }

        }
        return umaResource;
    }

}