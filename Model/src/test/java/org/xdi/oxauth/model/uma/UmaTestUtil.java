/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.uma;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponse;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Id;
import org.xdi.oxauth.model.uma.wrapper.Token;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UmaTestUtil {

    private UmaTestUtil() {
    }

    public static void assert_(UmaScopeDescription p_scopeDescription) {
        assertNotNull(p_scopeDescription, "Scope description is null");
        assertTrue(StringUtils.isNotBlank(p_scopeDescription.getName()), "Scope name is empty");
    }

    public static void assert_(RptIntrospectionResponse p_rptStatus) {
        assertNotNull(p_rptStatus, "Token response status is null");
        assertTrue(p_rptStatus.getActive(), "Token is not active");
        assertTrue(p_rptStatus.getPermissions() != null && !p_rptStatus.getPermissions().isEmpty(), "Permissions are empty.");
        assertNotNull(p_rptStatus.getExpiresAt(), "Expiration date is null");
    }

    public static void assert_(UmaMetadata metadata) {
        assertNotNull(metadata, "Metadata is null");
        assertTrue(ArrayUtils.contains(metadata.getGrantTypesSupported(), GrantType.OXAUTH_UMA_TICKET.getValue()));
        assertNotNull(metadata.getIssuer(), "Issuer isn't correct");
        assertNotNull(metadata.getTokenEndpoint(), "Token endpoint isn't correct");
        assertNotNull(metadata.getIntrospectionEndpoint(), "Introspection endpoint isn't correct");
        assertNotNull(metadata.getResourceRegistrationEndpoint(), "Resource registration endpoint isn't correct");
        assertNotNull(metadata.getPermissionEndpoint(), "Permission registration endpoint isn't correct");
        assertNotNull(metadata.getAuthorizationEndpoint(), "Authorization request endpoint isn't correct");
    }

    public static void assert_(Token token) {
        assertNotNull(token, "The token object is null");
        assertNotNull(token.getAccessToken(), "The access token is null");
        //assertNotNull(p_token.getRefreshToken(), "The refresh token is null");
    }

    public static void assert_(UmaResourceResponse resourceResponse) {
        assertNotNull(resourceResponse, "Resource status is null");
        assertNotNull(resourceResponse.getId(), "Resource description id is null");
    }

    public static UmaResource createResource() {
        final UmaResource resource = new UmaResource();
        resource.setName("Server Photo Album");
        resource.setIconUri("http://www.example.com/icons/flower.png");
        resource.setScopes(Arrays.asList("http://photoz.example.com/dev/scopes/view", "http://photoz.example.com/dev/scopes/all"));
        resource.setType("myType");
        return resource;
    }

    public static void assert_(PermissionTicket ticket) {
        assertNotNull(ticket, "Ticket is null");
        assertTrue(StringUtils.isNotBlank(ticket.getTicket()), "Ticket is empty");
    }

    public static void assert_(RPTResponse response) {
        assertNotNull(response, "RPT response is null");
        assertNotNull(response.getRpt(), "RPT is null");
    }

    public static void assert_(ClientResponse p_response) {
        assertNotNull(p_response, "Response is null");
        assertTrue(p_response.getStatus() == Response.Status.OK.getStatusCode(), "Response http code is not OK.");
    }

    public static void assert_(Id p_id) {
        assertNotNull(p_id, "ID is null");
        assertTrue(StringUtils.isNotBlank(p_id.getId()), "ID is blank");
    }

    public static void assert_(UmaTokenResponse response) {
        assertNotNull(response, "UMA Token response is null");
        assertNotNull(response.getAccessToken(), "RPT is null");
        assertNotNull(response.getPct(), "PCT is null");
    }

    public static void assert_(UmaNeedInfoResponse response) {
        assertNotNull(response, "UMA Need Info response is null");
        assertTrue(StringUtils.isNotBlank(response.getError()), "need_info error is blank");
        assertTrue(StringUtils.isNotBlank(response.getTicket()), "need_info ticket is blank");
        assertTrue(response.getRequiredClaims() != null && !response.getRequiredClaims().isEmpty(), "need_info required claims are empty");
        assertTrue(StringUtils.isNotBlank(response.getRedirectUser()), "need_info redirect user uri is blank");
    }

}
