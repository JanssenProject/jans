package io.swagger.client.api;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

import java.lang.reflect.Method;

/**
 *  Listener class to be attached to test classes to augment test ng behavior
 *
 * @author Shoeb
 */
public class TestMethodListener implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {
        Method method = iTestResult.getMethod().getConstructorOrMethod().getMethod();
        if (method == null) {
            return;
        }
        if (method.isAnnotationPresent(ProtectionAccessTokenRequired.class)) {
            if (!Tester.isTokenProtectionEnabled()) {
                iTestResult.setStatus(ITestResult.SKIP);
                throw new SkipException("Skipping the test as protection access token is not enabled. Ignore the exception.");
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult) {

    }

}
