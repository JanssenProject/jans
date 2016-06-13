package org.xdi.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Hold ImapPassword
 * 
 * @author Shekhar L
 */

@XmlRootElement
public class ImapPassword implements java.io.Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2631677803214869609L;

	@XmlElement(name = "encryptedString")
	private String encryptedString;

	@XmlElement(name = "cipher")
	private String cipher;

	@XmlElement(name = "mode")
	private String mode;

	public String getEncryptedString() {
		return encryptedString;
	}

	public void setEncryptedString(String encryptedString) {
		this.encryptedString = encryptedString;
	}

	public String getCipher() {
		return cipher;
	}

	public void setCipher(String cipher) {
		this.cipher = cipher;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

}
