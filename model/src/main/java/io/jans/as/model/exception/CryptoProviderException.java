/**
 * 
 */
package io.jans.as.model.exception;

/**
 * Exception, that is used by CryptoProvider suite classes  
 * 
 * @author Sergey Manoylo
 * @version October 27, 2021
 *
 */
public class CryptoProviderException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 4511613464367544458L;

    public CryptoProviderException(String message) {
        super(message);
    }

    public CryptoProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoProviderException(Throwable cause) {
        super(cause);
    }    

}
