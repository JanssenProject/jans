package org.xdi.oxauth.comp;

import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.config.StaticConfiguration;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.token.HandleTokenFactory;
import org.xdi.oxauth.service.CleanerTimer;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.InumService;
import org.xdi.service.CacheService;
import org.xdi.util.security.StringEncrypter;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
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

    @Test
    public void client_whichIsExpiredAndDeletable_MustBeRemoved() throws StringEncrypter.EncryptionException {
        // 1. create client
        final Client client = createDeletableClient();

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
        final Client client = createDeletableClient();
        client.setDeletable(false);

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
        final Client client = createDeletableClient();

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        client.setClientIdIssuedAt(calendar.getTime());

        calendar.add(Calendar.HOUR, 1);
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

    private Client createDeletableClient() throws StringEncrypter.EncryptionException {
        String clientsBaseDN = staticConfiguration.getBaseDn().getClients();

        String inum = inumService.generateClientInum();
        String generatedClientSecret = UUID.randomUUID().toString();

        final Client client = new Client();
        client.setDn("inum=" + inum + "," + clientsBaseDN);
        client.setClientName("Cleaner Timer Test");
        client.setClientId(inum);
        client.setClientSecret(clientService.encryptSecret(generatedClientSecret));
        client.setRegistrationAccessToken(HandleTokenFactory.generateHandleToken());
        client.setDeletable(true);
        return client;
    }
}
