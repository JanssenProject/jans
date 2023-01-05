/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.model.common.AccessToken;
import io.jans.as.server.model.common.ClientCredentialsGrant;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.ldap.TokenEntity;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.uma.authorization.UmaPCT;
import io.jans.as.server.uma.authorization.UmaRPT;
import io.jans.util.security.StringEncrypter;
import jakarta.ws.rs.WebApplicationException;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.*;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CleanerTimerTest extends BaseComponentTest {

    @Test(enabled = false) // disabled temporarily. It works perfectly locally but fails on jenkins. Reason is unclear.
    public void client_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        // 1. create client
        final Client client = createClient(true);

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        client.setClientIdIssuedAt(calendar.getTime());

        calendar.add(Calendar.MONTH, -1);
        client.setExpirationDate(calendar.getTime());

        getClientService().persist(client);

        // 2. client is in persistence
        assertNotNull(getClientService().getClient(client.getClientId()));

        // 3. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 4. no client in persistence
        assertNull(getClientService().getClient(client.getClientId()));
    }

    @Test
    public void client_whichIsExpiredAndNotDeletable_MustNotBeRemoved() throws StringEncrypter.EncryptionException {
        // 1. create client
        final Client client = createClient(false);

        try {
            final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            client.setClientIdIssuedAt(calendar.getTime());

            calendar.add(Calendar.HOUR, -1);
            client.setExpirationDate(calendar.getTime());

            getClientService().persist(client);

            // 2. client is in persistence
            assertNotNull(getClientService().getClient(client.getClientId()));

            // 3. clean up
            getCleanerTimer().processImpl();
            getCacheService().clear();

            // 4. client is in persistence (not removed)
            assertNotNull(getClientService().getClient(client.getClientId()));
        } finally {
            client.setDeletable(true); // make it available for cleaner
            getClientService().merge(client);
        }
    }

    @Test
    public void client_whichIsNotExpiredAndDeletable_MustNotBeRemoved() throws StringEncrypter.EncryptionException {

        // 1. create client
        final Client client = createClient(true);

        getClientService().persist(client);

        // 2. client is in persistence
        assertNotNull(getClientService().getClient(client.getClientId()));

        // 3. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 4. client is in persistence (not removed)
        assertNotNull(getClientService().getClient(client.getClientId()));
    }

    @Test
    public void token_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        final Client client = createClient();
        getClientService().persist(client);

        // 1. create token
        final ClientCredentialsGrant grant = getAuthorizationGrantList().createClientCredentialsGrant(new User(), client);
        final AccessToken accessToken = grant.createAccessToken(new ExecutionContext(null, null));

        // 2. token exists
        assertNotNull(getGrantService().getGrantByCode(accessToken.getCode()));

        // 3. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 4. token exists
        final TokenEntity grantLdap = getGrantService().getGrantByCode(accessToken.getCode());
        assertNotNull(grantLdap);

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        grantLdap.setExpirationDate(calendar.getTime());

        getGrantService().merge(grantLdap);

        // 5. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 6. no token in persistence
        assertNull(getGrantService().getGrantByCode(accessToken.getCode()));
    }

    @Test
    public void umaRpt_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        final Client client = createClient();

        getClientService().persist(client);

        // 1. create RPT
        final ExecutionContext executionContext = new ExecutionContext(null, null);
        executionContext.setClient(client);

        final UmaRPT rpt = getUmaRptService().createRPTAndPersist(executionContext, Lists.newArrayList());

        // 2. RPT exists
        assertNotNull(getUmaRptService().getRPTByCode(rpt.getNotHashedCode()));

        // 3. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 4. RPT exists
        assertNotNull(getUmaRptService().getRPTByCode(rpt.getNotHashedCode()));

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.MINUTE, -10);
        rpt.setExpirationDate(calendar.getTime());

        getUmaRptService().merge(rpt);

        // 5. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 6. no RPT in persistence
        assertNull(getUmaRptService().getRPTByCode(rpt.getNotHashedCode()));
    }

    @Test
    public void umaResource_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        final Client client = createClient();

        getClientService().persist(client);

        // 1. create resource
        UmaResource resource = new UmaResource();
        resource.setName("Test resource");
        resource.setScopes(Lists.newArrayList("view"));
        resource.setId(UUID.randomUUID().toString());
        resource.setDn(getUmaResourceService().getDnForResource(resource.getId()));

        final Calendar calendar = Calendar.getInstance();
        resource.setCreationDate(calendar.getTime());

        getUmaResourceService().addResource(resource);

        // 2. resource exists
        assertNotNull(getUmaResourceService().getResourceById(resource.getId()));

        // 3. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 4. resource exists
        assertNotNull(getUmaResourceService().getResourceById(resource.getId()));

        calendar.add(Calendar.MINUTE, -10);
        resource.setExpirationDate(calendar.getTime());

        getUmaResourceService().updateResource(resource, true);

        // 5. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 6. no resource in persistence
        try {
            getUmaResourceService().getResourceById(resource.getId());
            throw new AssertionError("Test failed, no 404 exception");
        } catch (WebApplicationException e) {
            // we expect WebApplicationException 404 here
            assertEquals(e.getResponse().getStatus(), 404);
        }
    }

    @Test
    public void umaPermission_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        final Client client = createClient();

        getClientService().persist(client);

        final String ticket = UUID.randomUUID().toString();

        // 1. create permission
        UmaPermission permission = new UmaPermission();
        permission.setTicket(ticket);
        permission.setConfigurationCode(UUID.randomUUID().toString());
        permission.setResourceId(UUID.randomUUID().toString());

        getUmaPermissionService().addPermission(permission, client.getDn());

        // 2. permission exists
        assertNotNull(getUmaPermissionService().getPermissionsByTicket(ticket).get(0));

        // 3. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 4. permission exists
        assertNotNull(getUmaPermissionService().getPermissionsByTicket(ticket).get(0));

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        permission.setExpirationDate(calendar.getTime());

        getUmaPermissionService().merge(permission);

        // 5. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 6. no permission in persistence
        final List<UmaPermission> permissionsByTicket = getUmaPermissionService().getPermissionsByTicket(ticket);
        assertTrue(permissionsByTicket.isEmpty());
    }

    @Test
    public void umaPct_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        final Client client = createClient();
        getClientService().persist(client);

        // 1. create pct
        UmaPCT pct = getUmaPctService().createPct(client.getClientId());
        getUmaPctService().persist(pct);

        // 2. pct exists
        assertNotNull(getUmaPctService().getByCode(pct.getCode()));

        // 3. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 4. pct exists
        assertNotNull(getUmaPctService().getByCode(pct.getCode()));

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -10);
        pct.setExpirationDate(calendar.getTime());

        getUmaPctService().merge(pct);

        // 5. clean up
        getCleanerTimer().processImpl();
        getCacheService().clear();

        // 6. no pct in persistence
        assertNull(getUmaPctService().getByCode(pct.getCode()));
    }

    private Client createClient() throws StringEncrypter.EncryptionException {
        return createClient(true);
    }

    private Client createClient(boolean deletable) throws StringEncrypter.EncryptionException {
        String clientsBaseDN = getStaticConfiguration().getBaseDn().getClients();

        String inum = getInumService().generateClientInum();
        String generatedClientSecret = UUID.randomUUID().toString();

        final Client client = new Client();
        client.setDn("inum=" + inum + "," + clientsBaseDN);
        client.setClientName("Cleaner Timer Test");
        client.setClientId(inum);
        client.setClientSecret(getClientService().encryptSecret(generatedClientSecret));
        client.setRegistrationAccessToken(HandleTokenFactory.generateHandleToken());
        client.setDeletable(deletable);

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        client.setClientIdIssuedAt(calendar.getTime());

        calendar.add(Calendar.MINUTE, 10);
        client.setExpirationDate(calendar.getTime());
        return client;
    }
}
