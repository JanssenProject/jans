/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The bounded thread pools of the batch ingest path: one verifies the elements of a batch in
 * parallel ({@code traceConfiguration.batchVerifyThreads}), one runs the correlation work after a
 * batch commit ({@code traceConfiguration.postCommitCorrelationThreads}). A thread count of
 * {@code 0} means no pool: the caller does the work in its own thread.
 *
 * <p>Each pool has a bounded queue. A task that does not fit, or that arrives while a pool is
 * being replaced, runs in the submitting thread, so no task is ever dropped and a busy pool slows
 * its callers down instead of growing. A changed thread count replaces the pool on the next call;
 * the old pool finishes the tasks it already has.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceIngestExecutors {

	static final int QUEUE_CAPACITY = 1024;

	static final long SHUTDOWN_WAIT_SECONDS = 10;

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	private final Pool verification = new Pool("trace-batch-verify");

	private final Pool correlation = new Pool("trace-post-commit");

	/**
	 * @return the pool that verifies batch elements, or {@code null} when they are verified in the
	 *         caller's thread
	 */
	public Executor verificationExecutor() {
		return verification.get(config().getBatchVerifyThreads());
	}

	/**
	 * @return the pool that runs post-commit correlation, or {@code null} when it runs in the
	 *         caller's thread before the response
	 */
	public Executor correlationExecutor() {
		return correlation.get(config().getPostCommitCorrelationThreads());
	}

	/**
	 * Stops both pools. Queued correlation work gets {@link #SHUTDOWN_WAIT_SECONDS} to finish; what
	 * is left after that is lost and logged.
	 */
	@PreDestroy
	public void shutdown() {
		verification.close();
		correlation.close();
	}

	private TraceConfiguration config() {
		return appConfiguration.getTraceConfiguration();
	}

	private final class Pool {

		private final String name;

		private ThreadPoolExecutor executor;

		private Pool(String name) {
			this.name = name;
		}

		private synchronized Executor get(int threads) {
			if (executor != null && executor.getCorePoolSize() != threads) {
				log.info("TRACE {} pool resized: threads={} -> {}", name, executor.getCorePoolSize(), threads);
				executor.shutdown();
				executor = null;
			}
			if (executor == null && threads > 0) {
				executor = newExecutor(threads);
			}
			return executor;
		}

		private ThreadPoolExecutor newExecutor(int threads) {
			ThreadPoolExecutor created = new ThreadPoolExecutor(threads, threads, 60L, TimeUnit.SECONDS,
					new ArrayBlockingQueue<>(QUEUE_CAPACITY), threadFactory(), (task, pool) -> task.run());
			created.allowCoreThreadTimeOut(true);
			return created;
		}

		private ThreadFactory threadFactory() {
			AtomicInteger counter = new AtomicInteger();
			return task -> {
				Thread thread = new Thread(task, name + "-" + counter.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			};
		}

		private void close() {
			ThreadPoolExecutor closing;
			synchronized (this) {
				closing = executor;
				executor = null;
			}
			if (closing == null) {
				return;
			}
			closing.shutdown();
			try {
				if (!closing.awaitTermination(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
					List<Runnable> dropped = closing.shutdownNow();
					log.warn("TRACE {} pool stopped with {} queued tasks not run", name, dropped.size());
				}
			} catch (InterruptedException ex) {
				closing.shutdownNow();
				Thread.currentThread().interrupt();
			}
		}

	}

}
