package io.jans.lock.cedarling.model;

/**
 * @author Yuriy Movchan Date: 12/18/2025
 */
public class CedarlingPermission {

	private String action;
	private String resource;
	private String id;
	private String path;

	public CedarlingPermission(String action, String resource, String id, String path) {
		this.action = action;
		this.resource = resource;
		this.id = id;
		this.path = path;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return "CedarlingPermission [action=" + action + ", resource=" + resource + ", id=" + id + ", path=" + path
				+ "]";
	}

}
