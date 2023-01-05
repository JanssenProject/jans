package io.jans.fido2.exception;

/**
 * Exception Class for Attestation related exceptions.
 * Extended from Fido2RuntimeException
 *
 */
public class AttestationException extends Fido2RuntimeException{

	/**
	 * Constructor for AttestationException
	 * @param errorMessage String containing error message
	 */
	public AttestationException(String errorMessage) {
		super(errorMessage);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8418353186420784112L;

}
