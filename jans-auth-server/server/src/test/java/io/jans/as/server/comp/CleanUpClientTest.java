/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.BaseComponentTest;
import io.jans.as.server.service.ClientService;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.util.StringHelper;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.List;

/**
 * @author Yuriy Movchan
 * @version 0.1, 03/24/2014
 */

public class CleanUpClientTest extends BaseComponentTest {

    @Test
    @Parameters(value = "usedClients")
    public void cleanUpClient(String usedClients) {
        Assert.assertNotNull(usedClients);
        List<String> usedClientsList = Arrays.asList(StringHelper.split(usedClients, ",", true, false));
        output("Used clients: " + usedClientsList);

        int clientsResultSetSize = 50;

        int countResults = 0;
        int countRemoved = 0;
        boolean existsMoreClients = true;
        while (existsMoreClients && countResults < 10000) {
            List<Client> clients = getClientService().getAllClients(new String[]{"inum"}, clientsResultSetSize);

            existsMoreClients = clients.size() == clientsResultSetSize;
            countResults += clients.size();

            Assert.assertNotNull(clients);
            output("Found clients: " + clients.size());
            output("Total clients: " + countResults);

            for (Client client : clients) {
                String clientId = client.getClientId();
                if (!usedClientsList.contains(clientId)) {
                    try {
                        getClientService().remove(client);
                    } catch (EntryPersistenceException ex) {
                        output("Failed to remove client: " + ex.getMessage());
                    }
                    countRemoved++;
                }
            }
        }

        output("Removed clients: " + countRemoved);
    }
}
