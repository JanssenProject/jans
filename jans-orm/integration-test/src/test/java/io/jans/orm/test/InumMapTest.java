/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.SearchScope;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimleInumMap;
import io.jans.orm.test.model.Status;

/**
 * jans-link inum map entries search with objectClass and NOT filters. It's
 * automated version of SqlLinkSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class InumMapTest extends BaseOrmTest {

	private static final String INUM_MAP_BASE_DN = "ou=link-interception,o=site";

	private SimleInumMap activeInumMap;
	private SimleInumMap inactiveInumMap;
	private String marker;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		for (SimleInumMap inumMap : new SimleInumMap[] { activeInumMap, inactiveInumMap }) {
			if (inumMap != null) {
				try {
					entryManager.remove(inumMap.getDn(), SimleInumMap.class);
				} catch (Exception ex) {
					// Test cleanup should not fail test run
				}
			}
		}
	}

	@Test
	public void createInumMaps() {
		marker = "inum_map_" + getRandomInum();

		activeInumMap = buildInumMap(marker + "_active", Status.ACTIVE);
		entryManager.persist(activeInumMap);

		inactiveInumMap = buildInumMap(marker + "_inactive", Status.INACTIVE);
		entryManager.persist(inactiveInumMap);
	}

	@Test(dependsOnMethods = "createInumMaps")
	public void searchNotInactiveInumMaps() {
		Filter filterObjectClass = Filter.createEqualityFilter("objectClass", "jansInumMap");
		Filter filterStatus = Filter.createNOTFilter(Filter.createEqualityFilter("jansStatus", Status.INACTIVE.getValue()));
		Filter filterMarker = Filter.createSubstringFilter("inum", null, new String[] { marker }, null);
		Filter filter = Filter.createANDFilter(filterObjectClass, filterStatus, filterMarker);

		List<SimleInumMap> result = entryManager.findEntries(INUM_MAP_BASE_DN, SimleInumMap.class, filter, SearchScope.SUB, null,
				null, 0, 0, 1000);

		assertNotNull(result);
		assertEquals(result.size(), 1);
		assertEquals(result.get(0).getStatus(), Status.ACTIVE);
	}

	private SimleInumMap buildInumMap(String inum, Status status) {
		SimleInumMap inumMap = new SimleInumMap();
		inumMap.setDn(String.format("inum=%s,%s", inum, INUM_MAP_BASE_DN));
		inumMap.setInum(inum);
		inumMap.setPrimaryKeyValues(new String[] { inum });
		inumMap.setStatus(status);

		return inumMap;
	}

}
