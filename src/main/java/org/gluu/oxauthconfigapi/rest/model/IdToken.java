package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Size;

public class IdToken implements Serializable {

	private static final long serialVersionUID = 1L;

	 @Size(min=1)
	 private List<String> idTokenSigningAlgValuesSupported;
	 
	 @Size(min=1)
	 private List<String> idTokenEncryptionAlgValuesSupported;
	 
	 @Size(min=1)
	 private List<String> idTokenEncryptionEncValuesSupported;
	 
	public List<String> getIdTokenSigningAlgValuesSupported() {
		return idTokenSigningAlgValuesSupported;
	}
	
	public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
		this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
	}
	
	public List<String> getIdTokenEncryptionAlgValuesSupported() {
		return idTokenEncryptionAlgValuesSupported;
	}
	
	public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
		this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
	}
	
	public List<String> getIdTokenEncryptionEncValuesSupported() {
		return idTokenEncryptionEncValuesSupported;
	}
	
	public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
		this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
	}
		 
}
