/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.uma;

import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Id;
import io.jans.as.model.uma.wrapper.Token;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponse;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UmaTestUtil {

    private UmaTestUtil() {
    }

    public static void assertIt(UmaScopeDescription scopeDescription) {
        assertNotNull(scopeDescription, "Scope description is null");
        assertTrue(StringUtils.isNotBlank(scopeDescription.getName()), "Scope name is empty");
    }

    public static void assertIt(RptIntrospectionResponse rptStatus) {
        assertNotNull(rptStatus, "Token response status is null");
        assertTrue(rptStatus.getActive(), "Token is not active");
        assertTrue(rptStatus.getPermissions() != null && !rptStatus.getPermissions().isEmpty(), "Permissions are empty.");
        assertNotNull(rptStatus.getExpiresAt(), "Expiration date is null");
    }

    public static void assertIt(UmaMetadata metadata) {
        assertNotNull(metadata, "Metadata is null");
        assertTrue(ArrayUtils.contains(metadata.getGrantTypesSupported(), GrantType.OXAUTH_UMA_TICKET.getValue()));
        assertNotNull(metadata.getIssuer(), "Issuer isn't correct");
        assertNotNull(metadata.getTokenEndpoint(), "Token endpoint isn't correct");
        assertNotNull(metadata.getIntrospectionEndpoint(), "Introspection endpoint isn't correct");
        assertNotNull(metadata.getResourceRegistrationEndpoint(), "Resource registration endpoint isn't correct");
        assertNotNull(metadata.getPermissionEndpoint(), "Permission registration endpoint isn't correct");
        assertNotNull(metadata.getAuthorizationEndpoint(), "Authorization request endpoint isn't correct");
    }

    public static void assertIt(Token token) {
        assertNotNull(token, "The token object is null");
        assertNotNull(token.getAccessToken(), "The access token is null");
        //assertNotNull(p_token.getRefreshToken(), "The refresh token is null");
    }

    public static void assertIt(UmaResourceResponse resourceResponse) {
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

    public static void assertIt(PermissionTicket ticket) {
        assertNotNull(ticket, "Ticket is null");
        assertTrue(StringUtils.isNotBlank(ticket.getTicket()), "Ticket is empty");
    }

    public static void assertIt(RPTResponse response) {
        assertNotNull(response, "RPT response is null");
        assertNotNull(response.getRpt(), "RPT is null");
    }

    public static void assertIt(ClientResponse response) {
        assertNotNull(response, "Response is null");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Response http code is not OK.");
    }

    public static void assertIt(Id id) {
        assertNotNull(id, "ID is null");
        assertTrue(StringUtils.isNotBlank(id.getId()), "ID is blank");
    }

    public static void assertIt(UmaTokenResponse response) {
        assertNotNull(response, "UMA Token response is null");
        assertNotNull(response.getAccessToken(), "RPT is null");
        assertNotNull(response.getPct(), "PCT is null");
    }

    public static void assertIt(UmaNeedInfoResponse response) {
        assertNotNull(response, "UMA Need Info response is null");
        assertTrue(StringUtils.isNotBlank(response.getError()), "need_info error is blank");
        assertTrue(StringUtils.isNotBlank(response.getTicket()), "need_info ticket is blank");
        assertTrue(response.getRequiredClaims() != null && !response.getRequiredClaims().isEmpty(), "need_info required claims are empty");
        assertTrue(StringUtils.isNotBlank(response.getRedirectUser()), "need_info redirect user uri is blank");
    }

}
