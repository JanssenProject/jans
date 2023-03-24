/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.test;

import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.Id;
import io.jans.as.model.uma.*;
import io.jans.as.model.uma.wrapper.Token;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.testng.Assert;

import java.util.Arrays;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UmaTestUtil {

    private UmaTestUtil() {
    }

    public static void assertIt(UmaScopeDescription scopeDescription) {
        Assert.assertNotNull(scopeDescription, "Scope description is null");
        Assert.assertTrue(StringUtils.isNotBlank(scopeDescription.getName()), "Scope name is empty");
    }

    public static void assertIt(RptIntrospectionResponse rptStatus) {
        Assert.assertNotNull(rptStatus, "Token response status is null");
        Assert.assertTrue(rptStatus.getActive(), "Token is not active");
        Assert.assertTrue(rptStatus.getPermissions() != null && !rptStatus.getPermissions().isEmpty(), "Permissions are empty.");
        Assert.assertNotNull(rptStatus.getExpiresAt(), "Expiration date is null");
    }

    public static void assertIt(UmaMetadata metadata) {
        Assert.assertNotNull(metadata, "Metadata is null");
        Assert.assertTrue(ArrayUtils.contains(metadata.getGrantTypesSupported(), GrantType.OXAUTH_UMA_TICKET.getValue()));
        Assert.assertNotNull(metadata.getIssuer(), "Issuer isn't correct");
        Assert.assertNotNull(metadata.getTokenEndpoint(), "Token endpoint isn't correct");
        Assert.assertNotNull(metadata.getIntrospectionEndpoint(), "Introspection endpoint isn't correct");
        Assert.assertNotNull(metadata.getResourceRegistrationEndpoint(), "Resource registration endpoint isn't correct");
        Assert.assertNotNull(metadata.getPermissionEndpoint(), "Permission registration endpoint isn't correct");
        Assert.assertNotNull(metadata.getAuthorizationEndpoint(), "Authorization request endpoint isn't correct");
    }

    public static void assertIt(Token token) {
        Assert.assertNotNull(token, "The token object is null");
        Assert.assertNotNull(token.getAccessToken(), "The access token is null");
        //assertNotNull(p_token.getRefreshToken(), "The refresh token is null");
    }

    public static void assertIt(UmaResourceResponse resourceResponse) {
        Assert.assertNotNull(resourceResponse, "Resource status is null");
        Assert.assertNotNull(resourceResponse.getId(), "Resource description id is null");
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
        Assert.assertNotNull(ticket, "Ticket is null");
        Assert.assertTrue(StringUtils.isNotBlank(ticket.getTicket()), "Ticket is empty");
    }

    public static void assertIt(RPTResponse response) {
        Assert.assertNotNull(response, "RPT response is null");
        Assert.assertNotNull(response.getRpt(), "RPT is null");
    }

    public static void assertIt(ClientResponse response) {
        Assert.assertNotNull(response, "Response is null");
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Response http code is not OK.");
    }

    public static void assertIt(Id id) {
        Assert.assertNotNull(id, "ID is null");
        Assert.assertTrue(StringUtils.isNotBlank(id.getId()), "ID is blank");
    }

    public static void assertIt(UmaTokenResponse response) {
        Assert.assertNotNull(response, "UMA Token response is null");
        Assert.assertNotNull(response.getAccessToken(), "RPT is null");
        Assert.assertNotNull(response.getPct(), "PCT is null");
    }

    public static void assertIt(UmaNeedInfoResponse response) {
        Assert.assertNotNull(response, "UMA Need Info response is null");
        Assert.assertTrue(StringUtils.isNotBlank(response.getError()), "need_info error is blank");
        Assert.assertTrue(StringUtils.isNotBlank(response.getTicket()), "need_info ticket is blank");
        Assert.assertTrue(response.getRequiredClaims() != null && !response.getRequiredClaims().isEmpty(), "need_info required claims are empty");
        Assert.assertTrue(StringUtils.isNotBlank(response.getRedirectUser()), "need_info redirect user uri is blank");
    }

}
