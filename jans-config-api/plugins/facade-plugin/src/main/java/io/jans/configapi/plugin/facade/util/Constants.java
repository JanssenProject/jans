/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.util;

public class Constants {

    private Constants() {
    }

    public static final String INPUT_DIR = "facade.input.dir";
    public static final String OUTPUT_DIR = "facade.output.dir";
    public static final String POLICY_STORE_DIR = "facade.policystore.dir";
    public static final String JOB_ENABLED = "facade.job.enabled";

    public static final String DEFAULT_INPUT_DIR = "/opt/jans/jetty/jans-config-api/custom/config/facade/input";
    public static final String DEFAULT_OUTPUT_DIR = "/opt/jans/jetty/jans-config-api/custom/config/facade/output";

    public static final String DEFAULT_POLICY_STORE_DIR = "/opt/jans/jetty/jans-config-api/custom/config/input";

    public static final String RUN_FILE = "run.json";
    public static final String TRUTH_FILE = "truth.json";
    public static final String STATE_FILE = "state.json";
}
