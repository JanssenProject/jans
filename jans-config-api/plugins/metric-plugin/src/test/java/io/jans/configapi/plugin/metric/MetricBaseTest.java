/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.metric;

import io.jans.configapi.core.test.BaseTest;

import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

public class MetricBaseTest extends BaseTest {

    // Execute before each test is run
    @BeforeMethod
    public void before() {
        boolean isAvailable = isEndpointAvailable(propertiesMap.get("metricAppTypesUrl"), accessToken);
        log.info("Metric Plugin isAvailable:{}", isAvailable);
        if (!isAvailable) {
            throw new SkipException("Metric Plugin Not deployed");
        } else {
            log.info("Metric Plugin is Deployed");
        }
    }

}
