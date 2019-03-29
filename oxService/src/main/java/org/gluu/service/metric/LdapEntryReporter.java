/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.service.metric;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.apache.commons.lang.time.DateUtils;
import org.gluu.model.ApplicationType;
import org.gluu.model.metric.MetricType;
import org.gluu.model.metric.counter.CounterMetricData;
import org.gluu.model.metric.counter.CounterMetricEntry;
import org.gluu.model.metric.ldap.MetricEntry;
import org.gluu.model.metric.timer.TimerMetricData;
import org.gluu.model.metric.timer.TimerMetricEntry;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A reporter which outputs measurements to LDAP
 *
 * @author Yuriy Movchan Date: 08/03/2015
 */
public final class LdapEntryReporter extends ScheduledReporter {

    private final Clock clock;
    private final MetricService metricService;
    private Date startTime;

    /**
     * Returns a new {@link Builder} for {@link LdapEntryReporter}.
     *
     * @param registry
     *            the registry to report
     * @return a {@link Builder} instance for a {@link LdapEntryReporter}
     */
    public static Builder forRegistry(MetricRegistry registry, MetricService metricService) {
        return new Builder(registry, metricService);
    }

    /**
     * A builder for {@link LdapEntryReporter} instances. Defaults to using the
     * default locale and time zone, writing to {@code System.out}, converting rates
     * to events/second, converting durations to milliseconds, and not filtering
     * metrics.
     */
    public static final class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private TimeZone timeZone;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
        private MetricService metricService;

        private Builder(MetricRegistry registry, MetricService metricService) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.timeZone = TimeZone.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.metricService = metricService;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock
         *            a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit
         *            a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit
         *            a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter
         *            a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Builds a {@link LdapEntryReporter} with the given properties.
         *
         * @return a {@link LdapEntryReporter}
         */
        public LdapEntryReporter build() {
            return new LdapEntryReporter(registry, clock, timeZone, rateUnit, durationUnit, filter, metricService);
        }
    }

    private LdapEntryReporter(MetricRegistry registry, Clock clock, TimeZone timeZone, TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter,
            MetricService metricService) {
        super(registry, "ldap-reporter", filter, rateUnit, durationUnit);
        this.clock = clock;
        this.metricService = metricService;
        this.startTime = new Date();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        reportImpl(counters, timers);
    }

    private void reportImpl(SortedMap<String, Counter> counters, SortedMap<String, Timer> timers) {
        if (!metricService.isMetricReporterEnabled()) {
            return;
        }

        final Date currentRunTime = new Date(clock.getTime());

        List<MetricEntry> metricEntries = new ArrayList<MetricEntry>();
        if (counters != null && !counters.isEmpty()) {
            List<MetricEntry> result = builCounterEntries(counters, metricService.getRegisteredMetricTypes());
            metricEntries.addAll(result);
        }

        if (timers != null && !timers.isEmpty()) {
            List<MetricEntry> result = builTimerEntries(timers, metricService.getRegisteredMetricTypes());
            metricEntries.addAll(result);
        }

        // Remove 1 millisecond to avoid overlapping periods
        final Calendar cal = Calendar.getInstance();
        cal.setTime(currentRunTime);
        cal.add(Calendar.MILLISECOND, -1);
        final Date endTime = cal.getTime();

        Date creationTime = new Date();

        if (metricEntries.size() > 0) {
            addMandatoryAttributes(metricService, startTime, endTime, metricEntries, creationTime);
        }

        startTime = currentRunTime;

        metricService.add(metricEntries, creationTime);
    }

    private List<MetricEntry> builCounterEntries(SortedMap<String, Counter> counters, Set<MetricType> registeredMetricTypes) {
        List<MetricEntry> result = new ArrayList<MetricEntry>();

        Set<MetricType> currentRegisteredMetricTypes = new HashSet<MetricType>(registeredMetricTypes);
        for (MetricType metricType : currentRegisteredMetricTypes) {
            Counter counter = counters.get(metricType.getValue());
            if (counter != null) {
                long count = counter.getCount();

                // Remove to avoid writing not changed statistic
                // registeredMetricTypes.remove(metricType);

                CounterMetricData counterMetricData = new CounterMetricData(count);
                CounterMetricEntry counterMetricEntry = new CounterMetricEntry();
                counterMetricEntry.setMetricData(counterMetricData);
                counterMetricEntry.setMetricType(metricType);

                result.add(counterMetricEntry);
            }
        }

        return result;
    }

    private List<MetricEntry> builTimerEntries(SortedMap<String, Timer> timers, Set<MetricType> registeredMetricTypes) {
        List<MetricEntry> result = new ArrayList<MetricEntry>();

        for (MetricType metricType : registeredMetricTypes) {
            Timer timer = timers.get(metricType.getValue());
            if (timer != null) {
                Snapshot snapshot = timer.getSnapshot();

                TimerMetricData timerMetricData = new TimerMetricData(timer.getCount(), convertRate(timer.getMeanRate()),
                        convertRate(timer.getOneMinuteRate()), convertRate(timer.getFiveMinuteRate()), convertRate(timer.getFifteenMinuteRate()),
                        getRateUnit(), convertDuration(snapshot.getMin()), convertDuration(snapshot.getMax()), convertDuration(snapshot.getMean()),
                        convertDuration(snapshot.getStdDev()), convertDuration(snapshot.getMedian()), convertDuration(snapshot.get75thPercentile()),
                        convertDuration(snapshot.get95thPercentile()), convertDuration(snapshot.get98thPercentile()),
                        convertDuration(snapshot.get99thPercentile()), convertDuration(snapshot.get999thPercentile()), getDurationUnit());
                TimerMetricEntry timerMetricEntry = new TimerMetricEntry();
                timerMetricEntry.setMetricData(timerMetricData);
                timerMetricEntry.setMetricType(metricType);

                result.add(timerMetricEntry);
            }
        }

        return result;
    }

    private void addMandatoryAttributes(MetricService metricService, Date startTime, Date endTime, List<MetricEntry> metricEntries,
            Date creationTime) {
        for (MetricEntry metricEntry : metricEntries) {
            String id = metricService.getuUiqueIdentifier();
            String dn = metricService.buildDn(id, creationTime, ApplicationType.OX_AUTH);

            metricEntry.setId(id);
            metricEntry.setDn(dn);

            metricEntry.setApplicationType(ApplicationType.OX_AUTH);

            metricEntry.setStartDate(startTime);
            metricEntry.setEndDate(endTime);
            metricEntry.setCreationDate(creationTime);
            metricEntry.setExpirationDate(DateUtils.addDays(creationTime, metricService.getEntryLifetimeInDays()));
        }
    }

}
