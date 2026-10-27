/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleGroup;

/**
 * Group entry create and search by owner/member filters. It's automated version
 * of SqlSimpleGroupSample and SqlCheckGroupSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class GroupTest extends BaseOrmTest {

	private static final String GROUPS_BASE_DN = "ou=groups,o=jans";

	private SimpleGroup persistedGroup;
	private String ownerDn;
	private String memberDn;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager != null) && (persistedGroup != null)) {
			try {
				entryManager.remove(persistedGroup.getDn(), SimpleGroup.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createGroup() {
		String inum = getRandomInum();
		ownerDn = String.format("inum=owner_%s,ou=people,o=jans", inum);
		memberDn = String.format("inum=member_%s,ou=people,o=jans", inum);

		SimpleGroup group = new SimpleGroup();
		group.setDn(String.format("inum=%s,%s", inum, GROUPS_BASE_DN));
		group.setInum(inum);
		group.setDisplayName("Sample group " + inum);
		group.setOwner(ownerDn);
		group.setMembers(Arrays.asList(memberDn, String.format("inum=member2_%s,ou=people,o=jans", inum)));

		entryManager.persist(group);

		persistedGroup = group;
	}

	@Test(dependsOnMethods = "createGroup")
	public void searchGroupByOwnerOrMember() {
		Filter ownerFilter = Filter.createEqualityFilter("owner", ownerDn);
		Filter memberFilter = Filter.createEqualityFilter("member", memberDn).multiValued();
		Filter searchFilter = Filter.createORFilter(ownerFilter, memberFilter);

		List<SimpleGroup> result = entryManager.findEntries(GROUPS_BASE_DN, SimpleGroup.class, searchFilter);

		assertNotNull(result);
		assertEquals(result.size(), 1);
		assertEquals(result.get(0).getInum(), persistedGroup.getInum());
		assertEquals(result.get(0).getMembers().size(), 2);
	}

	@Test(dependsOnMethods = "createGroup")
	public void checkIsMemberOrOwner() {
		// Check by owner DN which is not group member
		Filter ownerFilter = Filter.createEqualityFilter("owner", ownerDn);
		Filter memberFilter = Filter.createEqualityFilter("member", ownerDn).multiValued();
		Filter searchFilter = Filter.createORFilter(ownerFilter, memberFilter);

		boolean isMemberOrOwner = entryManager.findEntries(persistedGroup.getDn(), SimpleGroup.class, searchFilter, 1).size() > 0;
		assertTrue(isMemberOrOwner);

		// Check by DN which is not group member or owner
		String otherPersonDn = "inum=other_person,ou=people,o=jans";
		Filter otherFilter = Filter.createORFilter(Filter.createEqualityFilter("owner", otherPersonDn),
				Filter.createEqualityFilter("member", otherPersonDn).multiValued());

		boolean isOtherMemberOrOwner = entryManager.findEntries(persistedGroup.getDn(), SimpleGroup.class, otherFilter, 1).size() > 0;
		assertEquals(isOtherMemberOrOwner, false);
	}

}
