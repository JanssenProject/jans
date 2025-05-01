/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */
package io.jans.configapi.core.test.listener;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.google.common.base.Throwables;

public class ApiUnitTestListener implements ITestListener {

    @Override
    public void onTestStart(ITestResult result) {
        Reporter.log("Test STARTED: " + getTestInfo(result), true);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        Reporter.log("Test SUCCESS: " + getTestInfo(result), true);
        Reporter.log("", true);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Reporter.log("Test FAILED: " + getTestInfo(result), true);
        testFailed(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Reporter.log("Test SKIPPED: " + getTestInfo(result), true);
        Reporter.log("", true);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        Reporter.log("Test FAILED with Success Percentage: " + getTestInfo(result), true);
        testFailed(result);
    }

    @Override
    public void onStart(ITestContext context) {
        Reporter.log("Test onStart ", true);
    }

    @Override
    public void onFinish(ITestContext context) {
        Reporter.log("Test onFinish ", true);
    }

    private void testFailed(ITestResult result) {
        Object[] parameters = result.getParameters();
        if (parameters != null) {
            Reporter.log("Test Parameters: ", true);
            for (Object parameter : parameters) {
                Reporter.log("parameter = " + parameter, true);
            }
        }
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            Reporter.log("", true);
            Reporter.log("Exception: ", true);
            Reporter.log(Throwables.getStackTraceAsString(result.getThrowable()), true);
            Reporter.log("", true);
        }
    }

    private String getTestInfo(ITestResult result) {
        return result.getInstanceName() + "." + result.getName();
    }

}
