package io.jans.as.client;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * @author Yuriy Zabrovarnyy
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private static final int maxRetryCount = 3;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < maxRetryCount) {
            System.out.println("Retrying " + result.getTestName() + ", method: " + result.getMethod() + ", retryCount: " + retryCount);
            retryCount++;
            return true;
        } else {
            result.getTestContext().getFailedTests().addResult(result);
        }
        return false;
    }
}
