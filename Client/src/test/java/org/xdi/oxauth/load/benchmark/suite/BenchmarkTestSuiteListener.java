/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark.suite;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.Reporter;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/07/2014
 */

public class BenchmarkTestSuiteListener implements ISuiteListener {

    private long start;

    @Override
    public void onStart(ISuite suite) {
    	this.start = System.currentTimeMillis();
    	Reporter.log("Suite '" + suite.getName() + "' started ...", true);
    }

    @Override
    public void onFinish(ISuite suite) {
        final long takes = (System.currentTimeMillis() - start) / 1000;
        Reporter.log("Suite '" + suite.getName() + "' finished in " + takes + " seconds", true);
    }
}
