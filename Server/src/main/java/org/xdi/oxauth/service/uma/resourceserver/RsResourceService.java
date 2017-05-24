/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.resourceserver;

import org.slf4j.Logger;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.model.uma.persistence.UmaResource;
import org.xdi.oxauth.service.uma.UmaScopeService;
import org.xdi.oxauth.service.uma.UmaResourceService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/07/2013
 */
@Stateless
@Named("umaRsResourceService")
public class RsResourceService {

    @Inject
    private Logger log;

    @Inject
    private UmaResourceService resourceService;

    @Inject
    private UmaScopeService umaScopeService;

    public UmaResource getResource(RsResourceType p_type) {
        final UmaResource criteria = new UmaResource();
        criteria.setDn(resourceService.getBaseDnForResource());
        criteria.setName(p_type.getValue());

        final List<UmaResource> ldapResources = resourceService
                .findResources(criteria);
        if (ldapResources == null || ldapResources.isEmpty()) {
            log.trace("No resource for type: {}", p_type);
            return createResource(p_type);
        } else {
            final int size = ldapResources.size();
            final UmaResource first = ldapResources.get(0);
            if (size > 1) {
                // it's allowed to keep only one internal resource set for id generation : remove rest of resources

                // skip first element
                for (int i = 1; i < size; i++) {
                    resourceService.remove(ldapResources.get(i));
                }
            }
            return first;
        }
    }

    public List<String> getScopeDns(RsScopeType... p_types) {
        return p_types != null ? getScopeDns(Arrays.asList(p_types)) : new ArrayList<String>();
    }

    public List<String> getScopeDns(List<RsScopeType> p_types) {
        final List<String> result = new ArrayList<String>();
        if (p_types != null) {
            for (RsScopeType t : p_types) {
                final UmaScopeDescription generateIdScope = umaScopeService.getInternalScope(t.getValue());
                if (generateIdScope != null) {
                    result.add(generateIdScope.getDn());
                }
            }
        }
        return result;
    }

    private UmaResource createResource(RsResourceType p_type) {
        log.trace("Creating new internal resource, type: {} ...", p_type);
        // Create resource set description branch if needed
        if (!resourceService.containsBranch()) {
            resourceService.addBranch();
        }

        final String rsid = String.valueOf(System.currentTimeMillis());


        final UmaResource s = new UmaResource();
        s.setId(rsid);
        s.setRev("1");
        s.setName(p_type.getValue());
        s.setDn(resourceService.getDnForResource(rsid));
        s.setScopes(getScopeDns(p_type.getScopeTypes()));

//            final Boolean addClient = appConfiguration.getUmaKeepClientDuringResourceRegistration();
//            if (addClient != null ? addClient : true) {
//                s.setClients(new ArrayList<String>(Arrays.asList(clientDn)));
//            }

        resourceService.addResource(s);
        log.trace("New internal resource set created, type: {}.", p_type);
        return s;
    }
}
