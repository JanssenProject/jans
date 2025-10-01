/**
 * 
 */
package io.jans.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Sergey Manoylo 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SmtpTest implements java.io.Serializable {
	
	private static final String DEF_DEFAULT_SUBJECT = "SMTP Configuration verification"; 
	
	private static final String DEF_DEFAULT_MESSAGE = "Mail to test SMTP configuration";	

	/**
	 * 
	 */
	private static final long serialVersionUID = -6810257793583269676L;

    private boolean sign = true;

    private String subject = DEF_DEFAULT_SUBJECT;
    
    private String message = DEF_DEFAULT_MESSAGE;

    /**
     * 
     * @return
     */
    public boolean getSign() {
        return sign;
    }

    /**
     * 
     * @param sign
     */
    public void setSign(final boolean sign) {
        this.sign = sign;
    }
    
    /**
     * 
     * @return
     */
    public String getSubject() {
    	return subject;
    }

    /**
     * 
     * @param subject
     */
    public void setSubject(final String subject) {
    	this.subject = subject;
    }

    /**
     * 
     * @return
     */
    public String getMessage() {
    	return message;
    }

    /**
     * 
     * @param message
     */
    public void setMessage(final String message) {
    	this.message = message;
    }
}
