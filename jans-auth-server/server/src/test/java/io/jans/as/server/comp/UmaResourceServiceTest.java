/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.model.token.HandleTokenFactory;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.uma.service.UmaResourceService;
import io.jans.util.security.StringEncrypter;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import jakarta.inject.Inject;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UmaResourceServiceTest extends BaseComponentTest {

    @Inject
    private StaticConfiguration staticConfiguration;
    @Inject
    private ClientService clientService;
    @Inject
    private InumService inumService;
    @Inject
    private UmaResourceService umaResourceService;

    @Test
    public void umaResource_independentFromDeletableFlag_shouldBeSearchable() throws StringEncrypter.EncryptionException {
        final Client client = createClient();

        clientService.persist(client);

        // 1. create resource
        UmaResource resource = new UmaResource();
        resource.setName("Test resource");
        resource.setScopes(Lists.newArrayList("view"));
        resource.setId(UUID.randomUUID().toString());
        resource.setDn(umaResourceService.getDnForResource(resource.getId()));
        resource.setDeletable(false);

        final Calendar calendar = Calendar.getInstance();
        resource.setCreationDate(calendar.getTime());

        umaResourceService.addResource(resource);

        // 2. resource exists
        assertNotNull(umaResourceService.getResourceById(resource.getId()));

        // 4. resource exists
        assertNotNull(umaResourceService.getResourceById(resource.getId()));

        calendar.add(Calendar.MINUTE, -10);
        resource.setExpirationDate(calendar.getTime());
        resource.setDeletable(true);

        umaResourceService.updateResource(resource, true);

        // resource exists
        assertNotNull(umaResourceService.getResourceById(resource.getId()));

        // remove it
        umaResourceService.remove(resource);
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
