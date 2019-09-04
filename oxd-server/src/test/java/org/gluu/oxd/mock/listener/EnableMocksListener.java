package org.gluu.oxd.mock.listener;

import org.testng.IAnnotationTransformer;
import org.testng.ITest;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Parameters;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class EnableMocksListener implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod){

        System.out.println("===============>"+testMethod.getName());
        System.out.println("***************>"+isTestDisabled(testMethod.getName()));
        if (isTestDisabled(testMethod.getName())) {
            annotation.setEnabled(false);
        } else {
            annotation.setEnabled(true);
        }
    }

    public boolean isTestDisabled(String testName){
        if(testName.startsWith("mock")) {
            return false;
        } else {
            return true;
        }
    }


}
