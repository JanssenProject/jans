package io.jans.configapi.core.test.listener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

public class SkipTestsListener implements IAnnotationTransformer {
    
  private Logger logger = LogManager.getLogger(getClass());
	
  public void transform(ITestAnnotation annotation, Class testClass,
      Constructor testConstructor, Method testMethod) {
  
      if (testMethod != null) {
      	  // Try to find SkipTest annotation at method level or at class level
      	  SkipTest skipper = testMethod.getAnnotation(SkipTest.class);
      	  
      	  if (skipper == null) {
      	  	  skipper = testMethod.getDeclaringClass().getAnnotation(SkipTest.class);
      	  } 
      	  
      	  if (skipper != null) {
      	  	  // Search for a match with value of BaseTest.persistenceType (it's computed before suite)
      	  	  boolean disable = Stream.of(skipper.databases())
      	  	      .map(PersistenceType::fromString).anyMatch(AlterSuiteListener.persistenceType::equals);
              
      	  	  if (disable) {
              	  logger.warn("Disabling test method {}", testMethod.getName());
      	          annotation.setEnabled(false);
              }
      	  }
      }
  }
  
}
