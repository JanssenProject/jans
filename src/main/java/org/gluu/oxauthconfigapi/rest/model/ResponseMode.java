/**
 * OAuth 2.0 Response Mode values that this OP supports.
 */
package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;
/**
 * @author Puja Sharma
 *
 */
public class ResponseMode implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@NotEmpty
	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
