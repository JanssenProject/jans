package io.jans.webauthn.exceptions;

/**
 * This is a superclass for WebAuthn-related errors and exceptions.
 * It is explicitly not supposed to handle error messages, but to be subclassed
 * according to the specific errors outlined in the WebAuthn spec. For instance,
 * a NotAllowedError will be returned when a "NotAllowedError" is mentioned in the
 * spec.
 * <p>
 * For code or unexpected behavior errors, use a VirgilException instead.
 */
public abstract class WebAuthnException extends Exception {
    public WebAuthnException() {
        super();
    }
}
