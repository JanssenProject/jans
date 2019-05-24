package org.oxauth.persistence.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Yuriy Zabrovarnyy
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientAttributes implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 213428216912083393L;

	@JsonProperty("tlsClientAuthSubjectDn")
	private String tlsClientAuthSubjectDn;

	public String getTlsClientAuthSubjectDn() {
		return tlsClientAuthSubjectDn;
	}

	public void setTlsClientAuthSubjectDn(String tlsClientAuthSubjectDn) {
		this.tlsClientAuthSubjectDn = tlsClientAuthSubjectDn;
	}

	@Override
	public String toString() {
		return "ClientAttributes{" + "tlsClientAuthSubjectDn='" + tlsClientAuthSubjectDn + '\'' + '}';
	}
}
