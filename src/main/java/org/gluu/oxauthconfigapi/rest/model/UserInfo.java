package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Size;

public class UserInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Size(min=1)
	private List<String> userInfoSigningAlgValuesSupported;
	
	@Size(min=1)
    private List<String> userInfoEncryptionAlgValuesSupported;
	
	@Size(min=1)
    private List<String> userInfoEncryptionEncValuesSupported;
    
	public List<String> getUserInfoSigningAlgValuesSupported() {
		return userInfoSigningAlgValuesSupported;
	}
	
	public void setUserInfoSigningAlgValuesSupported(List<String> userInfoSigningAlgValuesSupported) {
		this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
	}
	
	public List<String> getUserInfoEncryptionAlgValuesSupported() {
		return userInfoEncryptionAlgValuesSupported;
	}
	
	public void setUserInfoEncryptionAlgValuesSupported(List<String> userInfoEncryptionAlgValuesSupported) {
		this.userInfoEncryptionAlgValuesSupported = userInfoEncryptionAlgValuesSupported;
	}
	
	public List<String> getUserInfoEncryptionEncValuesSupported() {
		return userInfoEncryptionEncValuesSupported;
	}
	
	public void setUserInfoEncryptionEncValuesSupported(List<String> userInfoEncryptionEncValuesSupported) {
		this.userInfoEncryptionEncValuesSupported = userInfoEncryptionEncValuesSupported;
	}    
}
