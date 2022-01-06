/**
 * 
 */
package io.jans.as.server;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.google.common.base.Throwables;

/**
 * @author Sergey Manoylo
 * @version December 29, 2021
 */
public class JansUnitTestsTestsListener implements ITestListener {
    
    @Override       
    public void onFinish(ITestContext context) {
    }       

    @Override       
    public void onStart(ITestContext context) {
    }       

    @Override       
    public void onTestFailedButWithinSuccessPercentage(ITestResult context) {
    }       

    @Override       
    public void onTestFailure(ITestResult result) {
        Reporter.log("Test FAILED: " + result.getName() + "." + result.getMethod().getMethodName(), true);
        Object[] parameters = result.getParameters();
        if(parameters != null) {
            Reporter.log("Test Parameters: ", true);            
            for(Object parameter : parameters) {
                Reporter.log("parameter = " + parameter, true);           
            }
        }
        Throwable throwable = result.getThrowable();
        if(throwable != null) {
            Reporter.log("", true);            
            Reporter.log("Exception: ", true);
            Reporter.log(Throwables.getStackTraceAsString(result.getThrowable()), true);
            Reporter.log("", true);            
        }
    }       

    @Override       
    public void onTestSkipped(ITestResult result) {
    }       

    @Override       
    public void onTestStart(ITestResult result) {
        Reporter.log("Test STARTED: " + result.getName() + "." + result.getMethod().getMethodName(), true);
    }
    
    @Override       
    public void onTestSuccess(ITestResult result) {
        Reporter.log("Test SUCCESS: " + result.getName() + "." + result.getMethod().getMethodName(), true);
        Reporter.log("", true);
    }
}
