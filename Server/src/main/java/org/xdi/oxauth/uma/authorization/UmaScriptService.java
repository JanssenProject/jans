/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.uma.authorization;

import org.slf4j.Logger;
import org.xdi.oxauth.model.uma.persistence.UmaScopeDescription;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.external.ExternalUmaAuthorizationPolicyService;
import org.xdi.oxauth.uma.service.UmaScopeService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */
@Stateless
@Named("umaScriptService")
public class UmaScriptService {

    @Inject
    private Logger log;

    @Inject
    private UmaScopeService umaScopeService;

    @Inject
    private ExternalUmaAuthorizationPolicyService policyService;

    @Inject
	private AttributeService attributeService;

    public Set<String> getScriptDNs(List<UmaScopeDescription> scopes) {
        HashSet<String> result = new HashSet<String>();

        for (UmaScopeDescription scope : scopes) {
            List<String> authorizationPolicies = scope.getAuthorizationPolicies();
            if (authorizationPolicies != null) {
                result.addAll(authorizationPolicies);
            }
        }

        return result;
    }
}
