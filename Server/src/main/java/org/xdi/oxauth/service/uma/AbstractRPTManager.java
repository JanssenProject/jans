/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.uma;

import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.oxauth.model.common.AccessToken;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.util.INumGenerator;

import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/02/2013
 */

public abstract class AbstractRPTManager implements IRPTManager {

    private static final Log LOG = Logging.getLog(AbstractRPTManager.class);
    private static final String GAT_MARKER = "gat_";

    public UmaRPT createRPT(IAuthorizationGrant grant, String amHost, String aat, boolean isGat) {
        final AccessToken accessToken = (AccessToken) grant.getAccessToken(aat);

        try {
            String code = UUID.randomUUID().toString() + "/" + INumGenerator.generate(8);
            if (isGat) {
                code = GAT_MARKER + code;
            }
            return new UmaRPT(code, new Date(), accessToken.getExpirationDate(), grant.getUserId(), grant.getClientId(), amHost);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Failed to generate RPT, aat: " + aat, e);
        }
    }

    public static boolean isGat(String rptCode) {
        return rptCode.startsWith(GAT_MARKER);
    }
}
