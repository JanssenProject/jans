/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.resourceserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.uma.persistence.ResourceSet;
import org.xdi.oxauth.model.uma.persistence.ScopeDescription;
import org.xdi.oxauth.service.uma.ResourceSetService;
import org.xdi.oxauth.service.uma.ScopeService;
import org.xdi.oxauth.util.ServerUtil;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/07/2013
 */
@Scope(ScopeType.STATELESS)
@Name("umaRsResourceService")
@AutoCreate
public class RsResourceService {

    @Logger
    private Log log;
    @In
    private ResourceSetService resourceSetService;
    @In
    private ScopeService umaScopeService;

    public static RsResourceService instance() {
        return ServerUtil.instance(RsResourceService.class);
    }

    public ResourceSet getResource(RsResourceType p_type) {
        final ResourceSet criteria = new ResourceSet();
        criteria.setDn(resourceSetService.getBaseDnForResourceSet());
        criteria.setName(p_type.getValue());

        final List<ResourceSet> ldapResourceSets = resourceSetService
                .findResourceSets(criteria);
        if (ldapResourceSets == null || ldapResourceSets.isEmpty()) {
            log.trace("No resource set for type: {0}", p_type);
            return createResourceSet(p_type);
        } else {
            final int size = ldapResourceSets.size();
            final ResourceSet first = ldapResourceSets.get(0);
            if (size > 1) {
                // it's allowed to keep only one internal resource set for id generation : remove rest of resources

                // skip first element
                for (int i = 1; i < size; i++) {
                    resourceSetService.remove(ldapResourceSets.get(i));
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
                final ScopeDescription generateIdScope = umaScopeService.getInternalScope(t.getValue());
                if (generateIdScope != null) {
                    result.add(generateIdScope.getDn());
                }
            }
        }
        return result;
    }

    private ResourceSet createResourceSet(RsResourceType p_type) {
        log.trace("Creating new internal resource set, type: {0} ...", p_type);
        // Create resource set description branch if needed
        if (!resourceSetService.containsBranch()) {
            resourceSetService.addBranch();
        }

        final String rsid = String.valueOf(System.currentTimeMillis());


        final ResourceSet s = new ResourceSet();
        s.setId(rsid);
        s.setRev("1");
        s.setName(p_type.getValue());
        s.setDn(resourceSetService.getDnForResourceSet(rsid));
        s.setScopes(getScopeDns(p_type.getScopeTypes()));

//            final Boolean addClient = ConfigurationFactory.instance().getConfiguration().getUmaKeepClientDuringResourceSetRegistration();
//            if (addClient != null ? addClient : true) {
//                s.setClients(new ArrayList<String>(Arrays.asList(clientDn)));
//            }

        resourceSetService.addResourceSet(s);
        log.trace("New internal resource set created, type: {0}.", p_type);
        return s;
    }
}
