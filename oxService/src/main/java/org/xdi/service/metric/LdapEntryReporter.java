/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.service.metric;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.xdi.model.ApplicationType;
import org.xdi.model.metric.MetricType;
import org.xdi.model.metric.counter.CounterMetricData;
import org.xdi.model.metric.counter.CounterMetricEntry;
import org.xdi.model.metric.ldap.MetricEntry;
import org.xdi.model.metric.timer.TimerMetricData;
import org.xdi.model.metric.timer.TimerMetricEntry;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

/**
 * A reporter which outputs measurements to LDAP 
 *
 * @author Yuriy Movchan Date: 08/03/2015
 */
public class LdapEntryReporter extends ScheduledReporter {
    /**
     * Returns a new {@link Builder} for {@link LdapEntryReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link LdapEntryReporter}
     */
    public static Builder forRegistry(MetricRegistry registry, String metricServiceComponentName) {
        return new Builder(registry, metricServiceComponentName);
    }

    /**
     * A builder for {@link LdapEntryReporter} instances. Defaults to using the default locale and
     * time zone, writing to {@code System.out}, converting rates to events/second, converting
     * durations to milliseconds, and not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private TimeZone timeZone;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;
		private String metricServiceComponentName;

        private Builder(MetricRegistry registry, String metricServiceComponentName) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.timeZone = TimeZone.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.metricServiceComponentName = metricServiceComponentName;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
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
            return new LdapEntryReporter(registry,
                                       clock,
                                       timeZone,
                                       rateUnit,
                                       durationUnit,
                                       filter, metricServiceComponentName);
        }
    }

    private final Clock clock;
	private final String metricServiceComponentName;
	private Date startTime;

    private LdapEntryReporter(MetricRegistry registry,
                            Clock clock,
                            TimeZone timeZone,
                            TimeUnit rateUnit,
                            TimeUnit durationUnit,
                            MetricFilter filter, String metricServiceComponentName) {
        super(registry, "ldap-reporter", filter, rateUnit, durationUnit);
        this.clock = clock;
        this.metricServiceComponentName = metricServiceComponentName;
        this.startTime = new Date();
    }

    @SuppressWarnings("rawtypes")
	@Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        final Date currentRunTime = new Date(clock.getTime());

        if (!(Contexts.isEventContextActive() || Contexts.isApplicationContextActive())) {
			Lifecycle.beginCall();
		}

		MetricService metricService = (MetricService) Component.getInstance(metricServiceComponentName);


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
	        	registeredMetricTypes.remove(metricType);
	        	
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
				Snapshot snapshot = timer .getSnapshot();
	
				TimerMetricData timerMetricData = new TimerMetricData(
						timer.getCount(),
						convertRate(timer.getMeanRate()),
						convertRate(timer.getOneMinuteRate()),
						convertRate(timer.getFiveMinuteRate()),
						convertRate(timer.getFifteenMinuteRate()),
						getRateUnit(),
						convertDuration(snapshot.getMin()),
						convertDuration(snapshot.getMax()),
						getDurationUnit());
				TimerMetricEntry timerMetricEntry = new TimerMetricEntry();
				timerMetricEntry.setMetricData(timerMetricData);
				timerMetricEntry.setMetricType(metricType);

				result.add(timerMetricEntry);
        	}
        }

        return result;
	}

	private void addMandatoryAttributes(MetricService metricService, Date startTime, Date endTime, List<MetricEntry> metricEntries, Date creationTime) {
		for (MetricEntry metricEntry : metricEntries) {
			String id = metricService.getuUiqueIdentifier();
			String dn = metricService.buildDn(id, creationTime, ApplicationType.OX_AUTH);

			metricEntry.setId(id);
			metricEntry.setDn(dn);

			metricEntry.setApplicationType(ApplicationType.OX_AUTH);

			metricEntry.setStartDate(startTime);
			metricEntry.setEndDate(endTime);
			metricEntry.setCreationDate(creationTime);
        }
	}

}
