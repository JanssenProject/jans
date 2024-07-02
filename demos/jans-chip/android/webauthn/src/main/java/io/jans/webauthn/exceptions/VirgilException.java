package io.jans.webauthn.exceptions;

/**
 * VirgilException is the generic exception class for this WebAuthn library.
 * It should be used whenever a code error or unexpected behavior occurs
 * that is *not* outlined in the WebAuthn spec.
 * <p>
 * For any error outlined in the WebAuthn spec, use a subclass of WebAuthnException.
 */
public class VirgilException extends Exception {
    public VirgilException() {
        super();
    }

    public VirgilException(String message) {
        super(message);
    }

    public VirgilException(String message, Throwable e) {
        super(message, e);
    }
}
