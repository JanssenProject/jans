/**
 * 
 */
package io.jans.util.exception;

/**
 * Exception thrown from encryption failures
 *
 * @author ssudala
 */
public class EncryptionException extends Exception {
    /**
     * Serial UID
     */
    private static final long serialVersionUID = -7220454928814292801L;

    /**
     * Default constructor
     *
     * @param t Wrapped exception
     */
    public EncryptionException(final Throwable t) {
        super(t);
    }

    /**
     * Default constructor
     * 
     * @param message Exception message
     */
    public EncryptionException(String message) {
        super(message);
    }
}
