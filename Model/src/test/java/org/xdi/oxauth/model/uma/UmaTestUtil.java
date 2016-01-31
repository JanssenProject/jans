/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.xdi.oxauth.model.common.Id;
import org.xdi.oxauth.model.uma.wrapper.Token;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2013
 */

public class UmaTestUtil {

    private UmaTestUtil() {
    }

    public static void assert_(ScopeDescription p_scopeDescription) {
        assertNotNull(p_scopeDescription, "Scope description is null");
        assertTrue(StringUtils.isNotBlank(p_scopeDescription.getName()), "Scope name is empty");
    }


    public static void assert_(RptIntrospectionResponse p_rptStatus) {
        assertNotNull(p_rptStatus, "Token response status is null");
        assertTrue(p_rptStatus.getActive(), "Token is not active");
        assertTrue(p_rptStatus.getPermissions() != null && !p_rptStatus.getPermissions().isEmpty(), "Permissions are empty.");
        assertNotNull(p_rptStatus.getExpiresAt(), "Expiration date is null");
    }

    public static void assert_(UmaConfiguration configuration) {
        assertNotNull(configuration, "Meta data configuration is null");
        assertEquals(configuration.getVersion(), "1.0", "Version isn't correct");
        assertNotNull(configuration.getIssuer(), "Issuer isn't correct");
        assertEquals(configuration.getPatProfilesSupported(), new String[]{"bearer"}, "Supported PAT profiles aren't correct");
        assertEquals(configuration.getAatProfilesSupported(), new String[]{"bearer"}, "Supported AAT profiles aren't correct");
        assertTrue(Arrays.equals(configuration.getRptProfilesSupported(), new String[]{"bearer"}) ||
                Arrays.equals(configuration.getRptProfilesSupported(), new String[]{"https://docs.kantarainitiative.org/uma/profiles/uma-token-bearer-1.0"})
                , "Supported RPT profiles aren't correct");
        assertTrue(Arrays.asList(configuration.getPatGrantTypesSupported()).contains("authorization_code"), "Supported PAT grant types aren't correct");
        assertTrue(Arrays.asList(configuration.getAatGrantTypesSupported()).contains("authorization_code"), "Supported AAT grant types aren't correct");
        assertEquals(configuration.getClaimTokenProfilesSupported(), new String[]{"openid"}, "Supported claim profiles aren't correct");
        assertNotNull(configuration.getTokenEndpoint(), "Token endpoint isn't correct");
        assertNotNull(configuration.getGatEndpoint(), "Token endpoint isn't correct");
        assertNotNull(configuration.getIntrospectionEndpoint(), "Introspection endpoint isn't correct");
        assertNotNull(configuration.getResourceSetRegistrationEndpoint(), "Resource set registration endpoint isn't correct");
        assertNotNull(configuration.getPermissionRegistrationEndpoint(), "Permission registration endpoint isn't correct");
        assertNotNull(configuration.getRptEndpoint(), "RPT endpoint isn't correct");
        assertNotNull(configuration.getAuthorizationEndpoint(), "Authorization request endpoint isn't correct");
    }

    public static void assert_(Token p_token) {
        assertNotNull(p_token, "The token object is null");
        assertNotNull(p_token.getAccessToken(), "The access token is null");
        //assertNotNull(p_token.getRefreshToken(), "The refresh token is null");
    }

    public static void assert_(ResourceSetResponse p_status) {
        assertNotNull(p_status, "Resource set status is null");
        assertNotNull(p_status.getId(), "Resource set description id is null");
    }

    public static ResourceSet createResourceSet() {
        final ResourceSet resourceSet = new ResourceSet();
        resourceSet.setName("Server Photo Album");
        resourceSet.setIconUri("http://www.example.com/icons/flower.png");
        resourceSet.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));
        return resourceSet;
    }

    public static void assert_(PermissionTicket p_t) {
        assertNotNull(p_t, "Ticket is null");
        assertTrue(StringUtils.isNotBlank(p_t.getTicket()), "Ticket is empty");
    }

    public static void assert_(RPTResponse p_response) {
        assertNotNull(p_response, "Requester permission token response is null");
        assertNotNull(p_response.getRpt(), "Requester permission token is null");
    }

    public static void assert_(ClientResponse p_response) {
        assertNotNull(p_response, "Response is null");
        assertTrue(p_response.getStatus() == Response.Status.OK.getStatusCode(), "Response http code is not OK.");
    }

    public static void assertAuthorizationRequest(RptAuthorizationResponse p_response) {
        assertNotNull(p_response, "Response is null");
        assertNotNull(p_response.getRpt(), "Rpt is null");
    }

    public static void assert_(Id p_id) {
        assertNotNull(p_id, "ID is null");
        assertTrue(StringUtils.isNotBlank(p_id.getId()), "ID is blank");
    }
}
