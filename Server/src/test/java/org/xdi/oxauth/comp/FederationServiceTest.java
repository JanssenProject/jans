/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.federation.FederationTrust;
import org.xdi.oxauth.model.federation.FederationTrustStatus;
import org.xdi.oxauth.model.util.Pair;
import org.xdi.oxauth.service.FederationDataService;
import org.xdi.oxauth.service.InumService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 12/10/2012
 */

public class FederationServiceTest extends BaseComponentTest {

    private String m_redirectUri;
    private String serverUri;

    private FederationTrust m_trust;
	
    @Parameters({"redirectUri", "serverUri"})
    public FederationServiceTest(String p_redirectUri, String serverUri) {
    	this.m_redirectUri = p_redirectUri;
        this.serverUri = serverUri;
    }

    @Override
    public void beforeClass() {
        final InumService inumService = (InumService) Component.getInstance(InumService.class);
        final Pair<String, String> pair = inumService.generateNewDN(ConfigurationFactory.instance().getBaseDn().getFederationTrust());

        m_trust = new FederationTrust();
        m_trust.setId(pair.getFirst());
        m_trust.setDn(pair.getSecond());
        m_trust.setDisplayName("Test trust1");
        m_trust.setFederationId("@!1111!0008!00F1!0001");
        m_trust.setFederationMetadataUri(serverUri + "/oxauth/seam/resource/restv1/oxauth/federationmetadata");
        m_trust.setRedirectUris(Arrays.asList(m_redirectUri));
        m_trust.setTrustStatus("inactive_by_checker");
        m_trust.setSkipAuthorization(true);
        m_trust.setScopes(Arrays.asList(
                "inum=@!1111!0009!BC01,ou=scopes,o=@!1111,o=gluu",
                "inum=@!1111!0009!2B41,ou=scopes,o=@!1111,o=gluu"
        ));

        getLdapManager().persist(m_trust);
    }

    @Override
    public void afterClass() {
        if (m_trust != null) {
            getLdapManager().remove(m_trust);
        }
    }

    @Test
    public void findTrustByRedirectURI() {
        final FederationDataService service = FederationDataService.instance();
        final List<String> redirectUriList = Arrays.asList(m_redirectUri);

        final List<FederationTrust> trust = service.getTrustByAnyRedirectUri(redirectUriList, FederationTrustStatus.INACTIVE_BY_CHECKER);
        Assert.assertTrue(trust != null && StringUtils.isNotBlank(trust.get(0).getFederationId()) && trust.get(0).getSkipAuthorization());

        final List<FederationTrust> noTrust = service.getTrustByAnyRedirectUri(Arrays.asList("http://no.no"), FederationTrustStatus.ACTIVE);
        Assert.assertTrue(noTrust == null || noTrust.isEmpty());

        final List<FederationTrust> all = service.getTrustByAnyRedirectUri(redirectUriList, null);
        Assert.assertTrue(all != null && StringUtils.isNotBlank(all.get(0).getFederationId()) && all.get(0).getSkipAuthorization());
    }

    @Test
    public void filterString() {
        final List<String> list = Arrays.asList("http://a.com", "http://b.com");
        final String filter = FederationDataService.createFilter(list);
        Assert.assertEquals(filter, "|(oxAuthRedirectURI=http://a.com)(oxAuthRedirectURI=http://b.com)");
    }
}
