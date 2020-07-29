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

}
