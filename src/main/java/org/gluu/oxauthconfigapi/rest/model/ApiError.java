/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class ApiError implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3836623519481821884L;
	String code;
	String message;
	String description;

	public ApiError() {
	}

	public ApiError(String code, String message, String description) {
		super();
		this.code = code;
		this.message = message;
		this.description = description;
	}

	@Override
	public String toString() {
		return "ApiError [code=" + code + ", message=" + message + ", description=" + description + "]";
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
