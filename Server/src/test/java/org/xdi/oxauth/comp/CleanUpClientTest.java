/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.comp;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.util.StringHelper;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.xdi.oxauth.BaseComponentTest;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.ClientService;

/**
 * @author Yuriy Movchan
 * @version 0.1, 03/24/2014
 */

public class CleanUpClientTest extends BaseComponentTest {

	@Inject
	private ClientService clientService;

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
			List<Client> clients = clientService.getAllClients(new String[] { "inum" }, clientsResultSetSize);

			existsMoreClients = clients.size() == clientsResultSetSize;
			countResults += clients.size();

			Assert.assertNotNull(clients);
			output("Found clients: " + clients.size());
			output("Total clients: " + countResults);

			for (Client client : clients) {
				String clientId = client.getClientId();
				if (!usedClientsList.contains(clientId)) {
					try {
						clientService.remove(client);
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
