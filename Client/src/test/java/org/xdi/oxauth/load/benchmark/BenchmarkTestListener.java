/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

/**
 * @author Yuriy Movchan
 * @version 0.1, 03/17/2015
 */
public class BenchmarkTestListener implements ITestListener {

	@Override
	public void onTestStart(ITestResult result) {
	}

	@Override
	public void onTestSuccess(ITestResult result) {
	}

	@Override
	public void onTestFailure(ITestResult result) {
	}

	@Override
	public void onTestSkipped(ITestResult result) {
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
	}

	@Override
	public void onStart(ITestContext context) {
		Reporter.log("Test '" + context.getName() + "' started ...");
	}

	@Override
	public void onFinish(ITestContext context) {
        final long takes = (context.getEndDate().getTime() - context.getStartDate().getTime()) / 1000;
        Reporter.log("Test '" + context.getName() + "' finished in " + takes + " seconds");
	}
}
