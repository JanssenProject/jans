package org.gluu.oxauthconfigapi.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.gluu.oxauthconfigapi.rest.model.Logging;
import org.junit.jupiter.api.Test;

public class LoggingTest {
	
	private Logging objectToTest;
	
	@Test
	public void newInstance() {

		objectToTest = new Logging();

		assertFalse(objectToTest.isDisableJdkLogger());
	}

}
