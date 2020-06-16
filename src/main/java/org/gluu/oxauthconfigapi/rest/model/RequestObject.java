package org.gluu.oxauthconfigapi.rest.model;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.Size;

public class RequestObject implements Serializable {

	private static final long serialVersionUID = 1L;

	@Size(min=1)
	private List<String> requestObjectSigningAlgValuesSupported;
	
	@Size(min=1)
    private List<String> requestObjectEncryptionAlgValuesSupported;
	
	@Size(min=1)
    private List<String> requestObjectEncryptionEncValuesSupported;

	public List<String> getRequestObjectSigningAlgValuesSupported() {
		return requestObjectSigningAlgValuesSupported;
	}

	public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
		this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
	}

	public List<String> getRequestObjectEncryptionAlgValuesSupported() {
		return requestObjectEncryptionAlgValuesSupported;
	}

	public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
		this.requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported;
	}

	public List<String> getRequestObjectEncryptionEncValuesSupported() {
		return requestObjectEncryptionEncValuesSupported;
	}

	public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
		this.requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported;
	}
	
}
