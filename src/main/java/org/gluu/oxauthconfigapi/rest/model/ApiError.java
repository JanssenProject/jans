/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.model;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class ApiError {

	String code;
	String message;
	String description;

	public ApiError(String code, String message, String description) {
		super();
		this.code = code;
		this.message = message;
		this.description = description;
	}

}
