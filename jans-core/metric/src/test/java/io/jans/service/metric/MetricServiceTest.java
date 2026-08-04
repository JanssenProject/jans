/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.metric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;

import io.jans.model.ApplicationType;
import io.jans.model.metric.MetricType;
import io.jans.orm.PersistenceEntryManager;

/**
 * @author Yuriy Movchan Date: 08/04/2026
 */
public class MetricServiceTest {

    private static TestMetricService metricService;

    private static class TestMetricService extends MetricService {

        private static final long serialVersionUID = 1L;

        @Override
        public String baseDn() {
            return "ou=metric,o=jans";
        }

        @Override
        public MetricService getMetricServiceInstance() {
            return this;
        }

        @Override
        public boolean isMetricReporterEnabled() {
            return false;
        }

        @Override
        public ApplicationType getApplicationType() {
            return ApplicationType.OX_AUTH;
        }

        @Override
        public String getNodeIdentifier() {
            return "test-node";
        }

        @Override
        public PersistenceEntryManager getEntryManager() {
            return null;
        }
    }

    @BeforeAll
    public static void setUp() {
        metricService = new TestMetricService();
        // Long interval to make sure the reporter never fires during the test
        metricService.initTimer(3600, 1);
    }

    @AfterAll
    public static void tearDown() {
        metricService.close();
    }

    @Test
    public void sameMetricTypeShouldResolveToSameCounter() {
        Counter first = metricService.getCounter(MetricType.TOKEN_ACCESS_TOKEN_COUNT);
        Counter second = metricService.getCounter(MetricType.TOKEN_ACCESS_TOKEN_COUNT);

        assertSame(first, second);
    }

    @Test
    public void subTypeShouldResolveToSeparateCounter() {
        Counter plain = metricService.getCounter(MetricType.TOKEN_ID_TOKEN_COUNT);
        Counter subTyped = metricService.getCounter(MetricType.TOKEN_ID_TOKEN_COUNT, "client1");

        assertNotSame(plain, subTyped);

        metricService.incCounter(MetricType.TOKEN_ID_TOKEN_COUNT, "client1");

        assertEquals(0, plain.getCount());
        assertEquals(1, subTyped.getCount());
    }

    @Test
    public void nullSubTypeShouldResolveToSameCounterAsNoSubType() {
        Counter plain = metricService.getCounter(MetricType.TOKEN_REFRESH_TOKEN_COUNT);
        Counter nullSubTyped = metricService.getCounter(MetricType.TOKEN_REFRESH_TOKEN_COUNT, null);

        assertSame(plain, nullSubTyped);

        boolean found = metricService.getRegisteredMetrics().stream()
                .anyMatch(registration -> registration.getMetricType() == MetricType.TOKEN_REFRESH_TOKEN_COUNT
                        && registration.getMetricSubType() == null
                        && "tkn_refresh_token_count".equals(registration.getRegistryName()));

        assertTrue(found);
    }

    @Test
    public void registrationsShouldTrackTypeAndSubType() {
        metricService.getTimer(MetricType.USER_AUTHENTICATION_RATE, "ldap");

        boolean found = metricService.getRegisteredMetrics().stream()
                .anyMatch(registration -> registration.getMetricType() == MetricType.USER_AUTHENTICATION_RATE
                        && "ldap".equals(registration.getMetricSubType())
                        && "user_authentication_rate.ldap".equals(registration.getRegistryName()));

        assertTrue(found);
    }

    @Test
    public void timerShouldBeRegisteredOnce() {
        Timer first = metricService.getTimer(MetricType.DYNAMIC_CLIENT_REGISTRATION_RATE);
        Timer second = metricService.getTimer(MetricType.DYNAMIC_CLIENT_REGISTRATION_RATE);

        assertSame(first, second);
    }

    @Test
    public void buildDnShouldUseUtcPeriod() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2026, Calendar.AUGUST, 4, 12, 0, 0);
        Date creationDate = cal.getTime();

        String dn = metricService.buildDn("id1", creationDate, ApplicationType.OX_AUTH);

        assertEquals("uniqueIdentifier=id1,ou=202608,ou=jans_auth,ou=metric,o=jans", dn);
    }

}
