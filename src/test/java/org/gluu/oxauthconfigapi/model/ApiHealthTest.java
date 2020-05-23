package org.gluu.oxauthconfigapi.model;

import static org.junit.jupiter.api.Assertions.*;

import org.gluu.oxauthconfigapi.rest.model.ApiHealth;
import org.junit.jupiter.api.Test;

public class ApiHealthTest {

	private ApiHealth objectToTest;

	@Test
	public void newInstance() {
		boolean isRunning = true;
		String state = "up";

		objectToTest = new ApiHealth(isRunning, state);

		assertTrue(objectToTest.isRunning());
		assertEquals(state, objectToTest.getState());
	}

	@Test
	public void newEmptyInstance() {

		objectToTest = new ApiHealth();

		assertNotNull(objectToTest);
	}

	@Test
	public void isRunning() {

		objectToTest = new ApiHealth();

		assertFalse(objectToTest.isRunning());
	}

	@Test
	public void setRunning() {
		objectToTest = new ApiHealth();

		objectToTest.setRunning(true);

		assertTrue(objectToTest.isRunning());
	}

	@Test
	public void getState() {
		objectToTest = new ApiHealth();

		assertEquals(null, objectToTest.getState());
	}

	@Test
	public void setState() {
		objectToTest = new ApiHealth();
		String state = "Up";
		objectToTest.setState(state);

		assertEquals(state, objectToTest.getState());
	}

}
