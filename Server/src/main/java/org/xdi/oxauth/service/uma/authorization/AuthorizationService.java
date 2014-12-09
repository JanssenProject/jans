/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma.authorization;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.python.core.PyObject;
import org.xdi.model.ProgrammingLanguage;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.UnmodifiableAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.persistence.ResourceSetPermission;
import org.xdi.oxauth.model.uma.persistence.UmaPolicy;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.uma.PolicyService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.service.PythonService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/02/2013
 */
@Scope(ScopeType.STATELESS)
@Name("umaAuthorizationService")
@AutoCreate
public class AuthorizationService {

    private static final String PYTHON_CLASS_NAME = "PythonExternalAuthorization";

    @Logger
    private Log log;
    @In
    private PythonService pythonService;
    @In
    private PolicyService umaPolicyService;

    public boolean allowToAddPermission(AuthorizationGrant p_grant, UmaRPT p_rpt, ResourceSetPermission p_permission, HttpServletRequest httpRequest, RptAuthorizationRequest rptAuthorizationRequest) {
        log.trace("Check policies for permission, id: {0}", p_permission.getDn());
        final List<UmaPolicy> umaPolicies = umaPolicyService.loadPoliciesByScopeDns(p_permission.getScopeDns());
        if (umaPolicies == null || umaPolicies.isEmpty()) {
            log.trace("No policies protection, allowed to grant permission.");
            return true;
        } else {
            final UnmodifiableAuthorizationGrant unmodifiableAuthorizationGrant = new UnmodifiableAuthorizationGrant(p_grant);
            final AuthorizationContext context = new AuthorizationContext(p_rpt, p_permission, unmodifiableAuthorizationGrant, httpRequest, rptAuthorizationRequest.getClaims());
            for (UmaPolicy policy : umaPolicies) {
                // if at least one policy returns false then whole result is false
                if (!applyPolicy(policy, context)) {
                    log.trace("Reject access. Policy dn: {0}", policy.getDn());
                    return false;
                }
            }
            log.trace("All policies are ok, grant access.");
            return true;
        }
    }

    private boolean applyPolicy(UmaPolicy p_policy, AuthorizationContext p_context) {
        try {
            log.trace("Apply policy id: {0} ...", p_policy.getInum());

            final ProgrammingLanguage programmingLanguage = p_policy.getProgrammingLanguage();
            if (programmingLanguage != null) {
                switch (programmingLanguage) {
                    case PYTHON:
                        final IPolicyExternalAuthorization pythonAuthorization = createPythonAuthorization(p_policy.getPolicyScript());
                        if (pythonAuthorization != null) {
                            final boolean result = pythonAuthorization.authorize(p_context);
                            log.trace("Policy result: {0}", result);
                            return result;
                        }
                        break;
                    case JAVA_SCRIPT:
                        log.error("JavaScript is not supported! Please use python instead.");
                        break;
                }
            } else {
                log.error("Unable to identify programming language.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    private IPolicyExternalAuthorization createPythonAuthorization(String p_pythonScript) {
        try {
            if (StringUtils.isNotBlank(p_pythonScript)) {
                InputStream bis = null;
                try {
                    bis = new ByteArrayInputStream(p_pythonScript.getBytes(Util.UTF8_STRING_ENCODING));
                    final IPolicyExternalAuthorization result = pythonService.loadPythonScript(bis, PYTHON_CLASS_NAME, IPolicyExternalAuthorization.class,
                            new PyObject[]{});
                    if (result == null) {
                        log.error("Policy python script does not implement IPolicyExternalAuthorization interface or script is corrupted.");
                    }
                    return result;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                } finally {
                    IOUtils.closeQuietly(bis);
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        log.error("Failed to prepare python external authorization");
        log.info("Using FALSE external authorization class.");
        return PolicyExternalAuthorizationEnum.FALSE;
    }

    public static AuthorizationService instance() {
        return ServerUtil.instance(AuthorizationService.class);
    }
}
