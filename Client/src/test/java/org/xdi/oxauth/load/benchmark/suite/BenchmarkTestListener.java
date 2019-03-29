/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.load.benchmark.suite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.gluu.util.StringHelper;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.Reporter;

/**
 * @author Yuriy Movchan
 * @version 0.1, 03/17/2015
 */
public class BenchmarkTestListener implements ITestListener {
	
	private List<String> methodNames;
	private Map<String, Long> methodTakes;
	private Map<String, Long> methodInvoked;
	
	private Lock lock = new ReentrantLock();

	@Override
	public void onTestStart(ITestResult result) {
	}

	@Override
	public void onTestSuccess(ITestResult result) {
		final String methodName = result.getMethod().getMethodName();
		final long takes = result.getEndMillis() - result.getStartMillis();

		Long totalTakes;
		Long totalInvoked;
		
		lock.lock();
		try {
			if (methodTakes.containsKey(methodName)) {
				totalTakes = methodTakes.get(methodName);
				totalInvoked = methodInvoked.get(methodName);

				totalTakes += takes;
				totalInvoked++;
			} else {
				methodNames.add(methodName);

				totalTakes = takes;
				totalInvoked = 1L;
			}
			methodTakes.put(methodName, totalTakes);
			methodInvoked.put(methodName, totalInvoked);
		} finally {
			lock.unlock();
		}
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
		Reporter.log("Test '" + context.getName() + "' started ...", true);

		this.methodNames = new ArrayList<String>();
		this.methodTakes = new HashMap<String, Long>();
		this.methodInvoked = new HashMap<String, Long>();
	}

	@Override
	public void onFinish(ITestContext context) {
		final long takes = (context.getEndDate().getTime() - context.getStartDate().getTime()) / 1000;
        Reporter.log("Test '" + context.getName() + "' finished in " + takes + " seconds", true);
        Reporter.log("================================================================================", true);

        for (String methodName : this.methodNames) {
    		final long methodTakes = this.methodTakes.get(methodName);
    		final long methodInvoked = this.methodInvoked.get(methodName);
    		final long methodThreads = getMethodThreqads(context, methodName);

    		long oneExecutionMethodTakes = methodTakes == 0 ? 0 : methodTakes / methodInvoked;
    		Reporter.log("BENCHMARK REPORT | " + " Method: '" + methodName + "' | Takes:" + methodTakes + " | Invoked: " + methodInvoked + " | Threads: " + methodThreads + " | Average method execution: " + oneExecutionMethodTakes  , true);
        }
        Reporter.log("================================================================================", true);

	}

	private long getMethodThreqads(ITestContext context, String methodName) {
		ITestNGMethod[] allTestMethods = context.getAllTestMethods();
		for (int i = 0; i < allTestMethods.length; i++) {
			if (StringHelper.equalsIgnoreCase(allTestMethods[i].getMethodName(), methodName)) {
				return allTestMethods[i].getThreadPoolSize();
			}
		}

		return 1;
	}
}
