package org.gluu.oxauthconfigapi.rest.model;

public class ApiHealth {

	boolean running;
	String state;

	public ApiHealth() {

	}

	public ApiHealth(boolean running, String state) {
		super();
		this.running = running;
		this.state = state;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

}