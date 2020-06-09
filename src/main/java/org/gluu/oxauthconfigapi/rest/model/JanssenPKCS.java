/**
 * PKCS #11 configuration related to oxAuth
 */
package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;

/**
 * @author Puja Sharma
 *
 */
public class JanssenPKCS implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private String janssenPKCSGenerateKeyEndpoint;
	private String janssenPKCSSignEndpoint;
	private String janssenPKCSVerifySignatureEndpoint;
	private String janssenPKCSDeleteKeyEndpoint;
	private String janssenPKCSTestModeToken;
	
	public String getJanssenPKCSGenerateKeyEndpoint() {
		return janssenPKCSGenerateKeyEndpoint;
	}
	
	public void setJanssenPKCSGenerateKeyEndpoint(String janssenPKCSGenerateKeyEndpoint) {
		this.janssenPKCSGenerateKeyEndpoint = janssenPKCSGenerateKeyEndpoint;
	}
	
	public String getJanssenPKCSSignEndpoint() {
		return janssenPKCSSignEndpoint;
	}
	
	public void setJanssenPKCSSignEndpoint(String janssenPKCSSignEndpoint) {
		this.janssenPKCSSignEndpoint = janssenPKCSSignEndpoint;
	}
	
	public String getJanssenPKCSVerifySignatureEndpoint() {
		return janssenPKCSVerifySignatureEndpoint;
	}
	
	public void setJanssenPKCSVerifySignatureEndpoint(String janssenPKCSVerifySignatureEndpoint) {
		this.janssenPKCSVerifySignatureEndpoint = janssenPKCSVerifySignatureEndpoint;
	}
	
	public String getJanssenPKCSDeleteKeyEndpoint() {
		return janssenPKCSDeleteKeyEndpoint;
	}
	
	public void setJanssenPKCSDeleteKeyEndpoint(String janssenPKCSDeleteKeyEndpoint) {
		this.janssenPKCSDeleteKeyEndpoint = janssenPKCSDeleteKeyEndpoint;
	}
	
	public String getJanssenPKCSTestModeToken() {
		return janssenPKCSTestModeToken;
	}
	
	public void setJanssenPKCSTestModeToken(String janssenPKCSTestModeToken) {
		this.janssenPKCSTestModeToken = janssenPKCSTestModeToken;
	}
	
}
