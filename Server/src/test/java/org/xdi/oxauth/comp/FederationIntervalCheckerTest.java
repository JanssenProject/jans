/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import org.jboss.seam.contexts.Contexts;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.util.Pair;
import org.xdi.oxauth.service.FederationCheckTimer;
import org.xdi.oxauth.service.InumService;
import org.xdi.oxauth.util.ServerUtil;

import java.util.Arrays;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/11/2012
 */

public class FederationIntervalCheckerTest extends BaseComponentTest {

    private String m_redirectUri;
	private String serverUri;

    private FederationTrust m_invalidFederationUrlTrust;
    private FederationTrust m_invalidRedirectUrlTrust;

    @Parameters({"redirectUri", "serverUri"})
    public FederationIntervalCheckerTest(String p_redirectUri, String serverUri) {
        m_redirectUri = p_redirectUri;
        this.serverUri = serverUri;
    }

    @Override
    public void beforeClass() {
        final InumService inumService = ServerUtil.instance(InumService.class);

        final Pair<String, String> pair1 = inumService.generateNewDN(ConfigurationFactory.instance().getBaseDn().getFederationTrust());
        final Pair<String, String> pair2 = inumService.generateNewDN(ConfigurationFactory.instance().getBaseDn().getFederationTrust());

        m_invalidFederationUrlTrust = new FederationTrust();
        m_invalidFederationUrlTrust.setId(pair1.getFirst());
        m_invalidFederationUrlTrust.setDn(pair1.getSecond());
        m_invalidFederationUrlTrust.setDisplayName("Interval checker: invalid federation url");
        m_invalidFederationUrlTrust.setFederationId("@!1111!0008!00F1!0001");
        m_invalidFederationUrlTrust.setFederationMetadataUri("https://invalid.redirect.uri.com");
        m_invalidFederationUrlTrust.setRedirectUris(Arrays.asList(m_redirectUri));
        m_invalidFederationUrlTrust.setStatus(FederationTrustStatus.ACTIVE);
        m_invalidFederationUrlTrust.setSkipAuthorization(true);

        m_invalidRedirectUrlTrust = new FederationTrust();
        m_invalidRedirectUrlTrust.setId(pair2.getFirst());
        m_invalidRedirectUrlTrust.setDn(pair2.getSecond());
        m_invalidRedirectUrlTrust.setDisplayName("Interval checker: invalid redirect uri");
        m_invalidRedirectUrlTrust.setFederationId("@!1111!0008!00F1!0001");
        m_invalidRedirectUrlTrust.setFederationMetadataUri(serverUri + "/oxauth/seam/resource/restv1/oxauth/federationmetadata");
        m_invalidRedirectUrlTrust.setRedirectUris(Arrays.asList(m_redirectUri, "http://invalid.redirecturi.com"));
        m_invalidRedirectUrlTrust.setStatus(FederationTrustStatus.ACTIVE);
        m_invalidRedirectUrlTrust.setSkipAuthorization(true);

        getLdapManager().persist(m_invalidFederationUrlTrust);
        getLdapManager().persist(m_invalidRedirectUrlTrust);
    }

    @Override
    public void afterClass() {
        if (m_invalidFederationUrlTrust != null) {
            getLdapManager().remove(m_invalidFederationUrlTrust);
        }
        if (m_invalidRedirectUrlTrust != null) {
            getLdapManager().remove(m_invalidRedirectUrlTrust);
        }
    }

    /**
     * Federation metadata url is invalid. Therefore checker can't check whether data is valid and
     * sets status to invalid_by_checker.
     */
    @Test
    public void invalidMetadataUri() {
        final FederationCheckTimer checker = (FederationCheckTimer) Contexts.getApplicationContext().get(FederationCheckTimer.class);
        checker.cleanUpTrusts();
        //checker.process();

        // sleep 5 seconds because process() method is asynchronous
//        sleepSeconds(5);

        // since federation metadata url is invalid, checker must set status to inactive_by_checker
        final FederationTrust trust = getLdapManager().find(FederationTrust.class, m_invalidFederationUrlTrust.getDn());
        Assert.assertTrue(trust.getStatus() == FederationTrustStatus.INACTIVE_BY_CHECKER);
    }

    /**
     * Invalid redirect uri must be removed from trust object.
     */
    @Test
    public void invalidRedirectUriInTrust() {
        final FederationCheckTimer checker = (FederationCheckTimer) Contexts.getApplicationContext().get(FederationCheckTimer.class);
        checker.cleanUpTrusts();

        final FederationTrust trust = getLdapManager().find(FederationTrust.class, m_invalidRedirectUrlTrust.getDn());
        // todo check it Assert.assertEquals(trust.getRedirectUris().size(), 1);
        Assert.assertTrue(trust.getRedirectUris().contains(m_redirectUri));
    }
}
