/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.UmaResource;

/**
 * UMA resource create and search with multi-valued filter. It's automated version
 * of *UmaResourceSample.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class UmaResourceTest extends BaseOrmTest {

	private static final String UMA_BASE_DN = "ou=resources,ou=uma,o=jans";

	private UmaResource persistedResource;
	private String clientDn;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager != null) && (persistedResource != null)) {
			try {
				entryManager.remove(persistedResource.getDn(), UmaResource.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createUmaResource() {
		String inum = getRandomInum();
		clientDn = String.format("inum=%s,ou=clients,o=jans", inum);

		UmaResource resource = new UmaResource();
		resource.setDn(String.format("jansId=%s,%s", inum, UMA_BASE_DN));
		resource.setId(inum);
		resource.setName("Sample UMA resource " + inum);
		// jansUmaScope column is single valued in SQL schema
		resource.setScopes(Arrays.asList("view"));
		resource.setClients(Arrays.asList(clientDn));

		entryManager.persist(resource);

		persistedResource = resource;
	}

	@Test(dependsOnMethods = "createUmaResource")
	public void searchUmaResourceByAssociatedClient() {
		Filter filter = Filter.createEqualityFilter("jansAssociatedClnt", clientDn).multiValued();
		List<UmaResource> umaResources = entryManager.findEntries(UMA_BASE_DN, UmaResource.class, filter);

		assertNotNull(umaResources);
		assertEquals(umaResources.size(), 1);
		assertEquals(umaResources.get(0).getId(), persistedResource.getId());
		assertEquals(umaResources.get(0).getScopes().size(), 1);
	}

}
