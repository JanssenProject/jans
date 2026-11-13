/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;

class TraceIngestExecutorsTest {

	private TraceIngestExecutors executors;

	private TraceConfiguration config;

	@BeforeEach
	void setUp() throws Exception {
		AppConfiguration appConfiguration = new AppConfiguration();
		config = appConfiguration.getTraceConfiguration();
		executors = new TraceIngestExecutors();
		setField("log", LoggerFactory.getLogger(TraceIngestExecutors.class));
		setField("appConfiguration", appConfiguration);
	}

	@AfterEach
	void tearDown() {
		executors.shutdown();
	}

	private void setField(String name, Object value) throws Exception {
		Field field = TraceIngestExecutors.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(executors, value);
	}

	@Test
	void zeroThreadsMeansNoPool() {
		assertNull(executors.verificationExecutor());
		assertNull(executors.correlationExecutor());
	}

	@Test
	void configuredPoolIsReusedAndReplacedWhenTheCountChanges() {
		config.setBatchVerifyThreads(2);
		Executor first = executors.verificationExecutor();
		assertNotNull(first);
		assertSame(first, executors.verificationExecutor());

		config.setBatchVerifyThreads(3);
		Executor second = executors.verificationExecutor();
		assertNotSame(first, second);

		config.setBatchVerifyThreads(0);
		assertNull(executors.verificationExecutor());
	}

	@Test
	void threadCountIsClamped() {
		config.setPostCommitCorrelationThreads(-5);
		assertEquals(0, config.getPostCommitCorrelationThreads());
		config.setPostCommitCorrelationThreads(1000);
		assertEquals(TraceConfiguration.HARD_MAX_INGEST_THREADS, config.getPostCommitCorrelationThreads());
	}

	@Test
	void fullQueueRunsTheTaskInTheCallerThread() throws Exception {
		config.setPostCommitCorrelationThreads(1);
		Executor pool = executors.correlationExecutor();
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch started = new CountDownLatch(1);
		try {
			pool.execute(() -> {
				started.countDown();
				awaitQuietly(release);
			});
			assertTrue(started.await(10, TimeUnit.SECONDS));
			for (int i = 0; i < TraceIngestExecutors.QUEUE_CAPACITY; i++) {
				pool.execute(() -> {
				});
			}

			AtomicReference<Thread> ranOn = new AtomicReference<>();
			pool.execute(() -> ranOn.set(Thread.currentThread()));

			assertSame(Thread.currentThread(), ranOn.get());
		} finally {
			release.countDown();
		}
	}

	@Test
	void shutdownRunsTheQueuedTasksFirst() {
		config.setPostCommitCorrelationThreads(1);
		Executor pool = executors.correlationExecutor();
		AtomicInteger done = new AtomicInteger();
		for (int i = 0; i < 20; i++) {
			pool.execute(done::incrementAndGet);
		}

		executors.shutdown();

		assertEquals(20, done.get());
		assertNotSame(pool, executors.correlationExecutor(), "a new pool after shutdown");
	}

	@Test
	void taskGivenToAReplacedPoolStillRuns() {
		config.setPostCommitCorrelationThreads(1);
		Executor old = executors.correlationExecutor();
		config.setPostCommitCorrelationThreads(2);
		executors.correlationExecutor();

		AtomicReference<Thread> ranOn = new AtomicReference<>();
		old.execute(() -> ranOn.set(Thread.currentThread()));

		assertSame(Thread.currentThread(), ranOn.get());
	}

	private static void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
