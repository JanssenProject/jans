/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.load;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Polls the Lock VM once per second over ssh: CPU ticks of the Lock JVM, of all PostgreSQL and
 * Apache processes and of the whole VM, the WAL sync counter and the wait events of the non-idle
 * client sessions of the Lock database. The remote loop is a plain bash script fed to
 * {@code ssh <host> bash -s}; it stops when its output pipe closes or after one hour.
 *
 * <p>Line format: {@code S <epoch> <lock> <pg> <apache> <cpu busy> <cpu total> <counters>},
 * counters as {@code wal_sync=<n>,<wait event>=<sessions>,...} ({@code CPU} for an active session without a wait event).
 */
final class VmSampler implements AutoCloseable {

	private static final String SCRIPT = String.join("\n",
			"HZ=$(getconf CLK_TCK)",
			"LP=$(pgrep -f 'jetty.base=/opt/jans/jetty/jans-lock' | head -1)",
			"echo \"H $HZ $(nproc) $LP\"",
			"end=$((SECONDS+3600))",
			"while [ $SECONDS -lt $end ]; do",
			"  t=$(date +%s.%N)",
			"  read -r -a st < /proc/$LP/stat",
			"  l=$((st[13]+st[14]))",
			"  pa=$(cat /proc/[0-9]*/stat 2>/dev/null | awk '$2==\"(postgres)\"{p+=$14+$15} $2==\"(apache2)\"{a+=$14+$15} END{printf \"%d %d\", p, a}')",
			"  c=$(head -1 /proc/stat | awk '{t=0; for(i=2;i<=NF;i++) t+=$i; printf \"%d %d\", t-$5-$6, t}')",
			"  q=$(runuser -u postgres -- psql -d jansdb -AtX -F= -c \"select 'wal_sync', wal_sync from pg_stat_wal union all select coalesce(wait_event_type||':'||wait_event,'CPU'), count(*) from pg_stat_activity where datname=current_database() and backend_type='client backend' and state<>'idle' and pid<>pg_backend_pid() group by 1\" 2>/dev/null | tr '\\n' ',')",
			"  echo \"S $t $l $pa $c $q\" || exit 0",
			"  sleep 1",
			"done", "");

	static final class Sample {
		final double epoch;
		final long lockTicks;
		final long pgTicks;
		final long apacheTicks;
		final long cpuBusy;
		final long cpuTotal;
		final long walSync;
		final Map<String, Integer> waits;

		Sample(double epoch, long lockTicks, long pgTicks, long apacheTicks, long cpuBusy, long cpuTotal, long walSync,
				Map<String, Integer> waits) {
			this.epoch = epoch;
			this.lockTicks = lockTicks;
			this.pgTicks = pgTicks;
			this.apacheTicks = apacheTicks;
			this.cpuBusy = cpuBusy;
			this.cpuTotal = cpuTotal;
			this.walSync = walSync;
			this.waits = waits;
		}
	}

	/** Averages over the samples of one run; CPU in % of one core. */
	static final class Summary {
		double lockAvg = Double.NaN;
		double lockP90 = Double.NaN;
		double pgAvg = Double.NaN;
		double pgP90 = Double.NaN;
		double apacheAvg = Double.NaN;
		double vmAvg = Double.NaN;
		double walSyncPerS = Double.NaN;
		String topWaits = "";
		int intervals;
	}

	private final Process process;

	private final List<Sample> samples = Collections.synchronizedList(new ArrayList<>());

	private final CountDownLatch header = new CountDownLatch(1);

	private volatile int hz = 100;

	private volatile int cores = 1;

	private VmSampler(Process process) {
		this.process = process;
		Thread reader = new Thread(this::read, "vm-sampler");
		reader.setDaemon(true);
		reader.start();
	}

	static VmSampler start(String host) throws IOException {
		Process process = new ProcessBuilder("ssh", "-o", "BatchMode=yes", host, "bash", "-s")
				.redirectError(ProcessBuilder.Redirect.DISCARD).start();
		try (OutputStream in = process.getOutputStream()) {
			in.write(SCRIPT.getBytes(StandardCharsets.UTF_8));
		}
		return new VmSampler(process);
	}

	private void read() {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("H ")) {
					String[] parts = line.split(" ");
					hz = Integer.parseInt(parts[1]);
					cores = Integer.parseInt(parts[2]);
					header.countDown();
				} else if (line.startsWith("S ")) {
					Sample sample = parseSample(line);
					if (sample != null) {
						samples.add(sample);
					}
				}
			}
		} catch (IOException | RuntimeException ex) {
			// the run summary shows the missing samples
		}
	}

	static Sample parseSample(String line) {
		String[] parts = line.split(" ", 8);
		if (parts.length < 7) {
			return null;
		}
		try {
			long walSync = 0;
			Map<String, Integer> waits = new LinkedHashMap<>();
			if (parts.length == 8) {
				for (String item : parts[7].split(",")) {
					int eq = item.lastIndexOf('=');
					if (eq <= 0) {
						continue;
					}
					String key = item.substring(0, eq);
					long value = Long.parseLong(item.substring(eq + 1).trim());
					if ("wal_sync".equals(key)) {
						walSync = value;
					} else {
						waits.put(key, (int) value);
					}
				}
			}
			return new Sample(Double.parseDouble(parts[1]), Long.parseLong(parts[2]), Long.parseLong(parts[3]),
					Long.parseLong(parts[4]), Long.parseLong(parts[5]), Long.parseLong(parts[6]), walSync, waits);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/** Waits for the first sample, so a run starts with the sampler already running. */
	void awaitFirstSample(long timeoutMs) throws InterruptedException {
		header.await(timeoutMs, TimeUnit.MILLISECONDS);
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (samples.isEmpty() && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
	}

	int sampleCount() {
		return samples.size();
	}

	/** @return the summary of the samples with index {@code from} (inclusive) to the newest one */
	Summary summarize(int from) {
		List<Sample> window;
		synchronized (samples) {
			window = new ArrayList<>(samples.subList(Math.max(0, from), samples.size()));
		}
		return summarize(window, hz, cores);
	}

	static Summary summarize(List<Sample> window, int hz, int cores) {
		Summary summary = new Summary();
		List<Double> lock = new ArrayList<>();
		List<Double> pg = new ArrayList<>();
		List<Double> apache = new ArrayList<>();
		List<Double> vm = new ArrayList<>();
		Map<String, Integer> waitTotals = new LinkedHashMap<>();
		double seconds = 0;
		long wal = 0;
		for (int i = 1; i < window.size(); i++) {
			Sample a = window.get(i - 1);
			Sample b = window.get(i);
			double dt = b.epoch - a.epoch;
			if (dt <= 0) {
				continue;
			}
			seconds += dt;
			lock.add(Math.max(0, b.lockTicks - a.lockTicks) * 100.0 / hz / dt);
			pg.add(Math.max(0, b.pgTicks - a.pgTicks) * 100.0 / hz / dt);
			apache.add(Math.max(0, b.apacheTicks - a.apacheTicks) * 100.0 / hz / dt);
			long total = b.cpuTotal - a.cpuTotal;
			vm.add(total <= 0 ? 0 : (b.cpuBusy - a.cpuBusy) * 100.0 * cores / total);
			wal += Math.max(0, b.walSync - a.walSync);
			b.waits.forEach((k, v) -> waitTotals.merge(k, v, Integer::sum));
		}
		summary.intervals = lock.size();
		if (lock.isEmpty()) {
			return summary;
		}
		summary.lockAvg = avg(lock);
		summary.lockP90 = p90(lock);
		summary.pgAvg = avg(pg);
		summary.pgP90 = p90(pg);
		summary.apacheAvg = avg(apache);
		summary.vmAvg = avg(vm);
		summary.walSyncPerS = wal / seconds;
		int n = lock.size();
		summary.topWaits = waitTotals.entrySet().stream()
				.sorted((x, y) -> Integer.compare(y.getValue(), x.getValue())).limit(3)
				.map(e -> e.getKey() + " " + String.format(Locale.ROOT, "%.2f", e.getValue() / (double) n))
				.collect(Collectors.joining(";"));
		return summary;
	}

	private static double avg(List<Double> values) {
		return values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
	}

	private static double p90(List<Double> values) {
		List<Double> sorted = new ArrayList<>(values);
		Collections.sort(sorted);
		int index = (int) Math.ceil(sorted.size() * 0.9) - 1;
		return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
	}

	@Override
	public void close() {
		process.destroy();
		try {
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				process.destroyForcibly();
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
