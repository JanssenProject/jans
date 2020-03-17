package gluu.scim2.client.exception;

/**
 * SCIM initialization exception
 *
 * @author Yuriy Movchan Date: 08/08/2013
 */
public class ScimInitializationException extends RuntimeException {

    private static final long serialVersionUID = -6376075805135656133L;

    public ScimInitializationException(String message) {
        super(message);
    }

    public ScimInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

}
