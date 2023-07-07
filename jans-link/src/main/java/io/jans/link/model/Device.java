package io.jans.link.model;

public class Device {
	private String addedOn;

	private String id;

	private String nickName;

	private boolean soft = false;

	public String getAddedOn() {
		return addedOn;
	}

	public void setAddedOn(String addedOn) {
		this.addedOn = addedOn;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNickName() {
		return nickName;
	}

	@Override
	public String toString() {
		return "Device [addedOn=" + addedOn + ", id=" + id + ", nickName=" + nickName + ", soft=" + soft + "]";
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public boolean isSoft() {
		return soft;
	}

	public void setSoft(boolean soft) {
		this.soft = soft;
	}
}
