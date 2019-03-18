package org.xdi.oxauth.comp;

import org.testng.annotations.Test;
import org.testng.collections.Lists;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.token.HandleTokenFactory;
import org.xdi.oxauth.model.uma.persistence.UmaResource;
import org.xdi.oxauth.service.CleanerTimer;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.InumService;
import org.xdi.oxauth.uma.authorization.UmaRPT;
import org.xdi.oxauth.uma.service.UmaResourceService;
import org.xdi.oxauth.uma.service.UmaRptService;
import org.xdi.service.CacheService;
import org.xdi.util.security.StringEncrypter;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class CleanerTimerTest extends BaseComponentTest {

    @Inject
    private StaticConfiguration staticConfiguration;
    @Inject
    private ClientService clientService;
    @Inject
    private InumService inumService;
    @Inject
    private CleanerTimer cleanerTimer;
    @Inject
    private CacheService cacheService;
    @Inject
    private UmaRptService umaRptService;
    @Inject
    private UmaResourceService umaResourceService;

    @Test
    public void client_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        // 1. create client
        final Client client = createClient(true);

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        client.setClientIdIssuedAt(calendar.getTime());

        calendar.add(Calendar.HOUR, -1);
        client.setExpirationDate(calendar.getTime());

        clientService.persist(client);

        // 2. client is in persistence
        assertNotNull(clientService.getClient(client.getClientId()));

        // 3. clean up
        cleanerTimer.processImpl();
        cacheService.clear();

        // 4. no client in persistence
        assertNull(clientService.getClient(client.getClientId()));
    }

    @Test
    public void client_whichIsExpiredAndNotDeletable_MustNotBeRemoved() throws StringEncrypter.EncryptionException {
        // 1. create client
        final Client client = createClient(false);

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        client.setClientIdIssuedAt(calendar.getTime());

        calendar.add(Calendar.HOUR, -1);
        client.setExpirationDate(calendar.getTime());

        clientService.persist(client);

        // 2. client is in persistence
        assertNotNull(clientService.getClient(client.getClientId()));

        // 3. clean up
        cleanerTimer.processImpl();
        cacheService.clear();

        // 4. client is in persistence (not removed)
        assertNotNull(clientService.getClient(client.getClientId()));
    }

    @Test
    public void client_whichIsNotExpiredAndDeletable_MustNotBeRemoved() throws StringEncrypter.EncryptionException {

        // 1. create client
        final Client client = createClient(true);

        clientService.persist(client);

        // 2. client is in persistence
        assertNotNull(clientService.getClient(client.getClientId()));

        // 3. clean up
        cleanerTimer.processImpl();
        cacheService.clear();

        // 4. client is in persistence (not removed)
        assertNotNull(clientService.getClient(client.getClientId()));
    }

    @Test
    public void umaRpt_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        final Client client = createClient();

        clientService.persist(client);

        // 1. create RPT
        final UmaRPT rpt = umaRptService.createRPTAndPersist(client, Lists.newArrayList());

        // 2. RPT exists
        assertNotNull(umaRptService.getRPTByCode(rpt.getCode()));

        // 3. clean up
        cleanerTimer.processImpl();
        cacheService.clear();

        // 4. RPT exists
        assertNotNull(umaRptService.getRPTByCode(rpt.getCode()));

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.MINUTE, -10);
        rpt.setExpirationDate(calendar.getTime());

        umaRptService.merge(rpt);

        // 5. clean up
        cleanerTimer.processImpl();
        cacheService.clear();

        // 6. no RPT in persistence
        assertNull(umaRptService.getRPTByCode(rpt.getCode()));
    }

    @Test
    public void umaResource_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        final Client client = createClient();

        clientService.persist(client);

        // 1. create resource
        UmaResource resource = new UmaResource();
        resource.setName("Test resource");
        resource.setScopes(Lists.newArrayList("view"));
        resource.setId(UUID.randomUUID().toString());
        resource.setDn(umaResourceService.getDnForResource(resource.getId()));

        final Calendar calendar = Calendar.getInstance();
        resource.setCreationDate(calendar.getTime());

        umaResourceService.addResource(resource);

        // 2. resource exists
        assertNotNull(umaResourceService.getResourceById(resource.getId()));

        // 3. clean up
        cleanerTimer.processImpl();
        cacheService.clear();

        // 4. resource exists
        assertNotNull(umaResourceService.getResourceById(resource.getId()));

        calendar.add(Calendar.MINUTE, -10);
        resource.setExpirationDate(calendar.getTime());

        umaResourceService.updateResource(resource, true);

        // 5. clean up
        cleanerTimer.processImpl();
        cacheService.clear();

        // 6. no resource in persistence
        try {
            umaResourceService.getResourceById(resource.getId());
            throw new AssertionError("Test failed, no 404 exception");
        } catch (WebApplicationException e) {
            // we expect WebApplicationException 404 here
            assertEquals(404, e.getResponse().getStatus());
        }
    }

    private Client createClient() throws StringEncrypter.EncryptionException {
        return createClient(true);
    }

    private Client createClient(boolean deletable) throws StringEncrypter.EncryptionException {
        String clientsBaseDN = staticConfiguration.getBaseDn().getClients();

        String inum = inumService.generateClientInum();
        String generatedClientSecret = UUID.randomUUID().toString();

        final Client client = new Client();
        client.setDn("inum=" + inum + "," + clientsBaseDN);
        client.setClientName("Cleaner Timer Test");
        client.setClientId(inum);
        client.setClientSecret(clientService.encryptSecret(generatedClientSecret));
        client.setRegistrationAccessToken(HandleTokenFactory.generateHandleToken());
        client.setDeletable(deletable);

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        client.setClientIdIssuedAt(calendar.getTime());

        calendar.add(Calendar.MINUTE, 10);
        client.setExpirationDate(calendar.getTime());
        return client;
    }
}
