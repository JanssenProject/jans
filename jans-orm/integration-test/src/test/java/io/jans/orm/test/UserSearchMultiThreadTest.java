/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleUser;
import io.jans.orm.util.StringHelper;

/**
 * Concurrent user search from multiple threads. It's automated scaled down version
 * of *UserSearchSample load tests.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class UserSearchMultiThreadTest extends BaseOrmTest {

	private static final String PEOPLE_BASE_DN = "ou=people,o=jans";

	private static final int USER_COUNT = 2000;
	private static final int THREAD_COUNT = 10;
	private static final int THREAD_ITERATION_COUNT = 200;

	private String marker;
	private List<String> createdDns = new ArrayList<>();

	private AtomicLong successResult = new AtomicLong(0);
	private AtomicLong failedResult = new AtomicLong(0);
	private AtomicLong errorResult = new AtomicLong(0);

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		try {
			entryManager.remove(PEOPLE_BASE_DN, SimpleUser.class, Filter.createEqualityFilter("jansGuid", marker), USER_COUNT * 2);
		} catch (Exception ex) {
			// Test cleanup should not fail test run
		}
	}

	@Test
	public void createUsers() {
		marker = "search_" + getRandomInum();

		for (int i = 0; i < USER_COUNT; i++) {
			SimpleUser user = new SimpleUser();
			String inum = getRandomInum();
			user.setDn(String.format("inum=%s,%s", inum, PEOPLE_BASE_DN));
			user.setUserId(String.format("%s_user%06d", marker, i));
			user.getCustomAttributes().add(new CustomObjectAttribute("jansGuid", marker));

			entryManager.persist(user);
			createdDns.add(user.getDn());
		}
	}

	@Test(dependsOnMethods = "createUsers")
	public void searchUsersMultiThread() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
		for (int i = 0; i < THREAD_COUNT; i++) {
			executorService.execute(() -> {
				for (int j = 0; j < THREAD_ITERATION_COUNT; j++) {
					long userIndex = Math.round(Math.random() * (USER_COUNT - 1));
					String uid = String.format("%s_user%06d", marker, userIndex);
					try {
						Filter filter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"),
								StringHelper.toLowerCase(uid));
						List<SimpleUser> foundUsers = entryManager.findEntries(PEOPLE_BASE_DN, SimpleUser.class, filter);
						if (foundUsers.size() == 1) {
							successResult.incrementAndGet();
						} else {
							failedResult.incrementAndGet();
						}
					} catch (Throwable ex) {
						errorResult.incrementAndGet();
					}
				}
			});
		}

		executorService.shutdown();
		executorService.awaitTermination(120, TimeUnit.SECONDS);

		assertEquals(errorResult.get(), 0, "Search errors detected");
		assertEquals(failedResult.get(), 0, "Not found users detected");
		assertEquals(successResult.get(), THREAD_COUNT * THREAD_ITERATION_COUNT);
	}

}
