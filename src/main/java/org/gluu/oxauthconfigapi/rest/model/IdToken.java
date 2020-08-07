package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;

public class IdToken implements Serializable {

	private static final long serialVersionUID = 1L;

	 @NotNull
	 @Size(min=1)
	 private List<String> idTokenSigningAlgValuesSupported;
	 
	 @NotNull
	 @Size(min=1)
	 private List<String> idTokenEncryptionAlgValuesSupported;
	 
	 @NotNull
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
