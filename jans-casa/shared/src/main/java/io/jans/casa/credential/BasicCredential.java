package io.jans.casa.credential;

/**
 * A POJO holding basic data of an enrolled credential (authentication device).
 * @author jgomer
 */
public class BasicCredential {

    private String nickName;
    private long addedOn;

    /**
     * Creates an instances of this class using the values supplied in the parameters.
     * @param nickName Nickname of this credential
     * @param addedOn Timestamp describing the moment (wrt to UNIX epoch) in which the device was enrolled
     */
    public BasicCredential(String nickName, long addedOn) {
        this.nickName = nickName;
        this.addedOn = addedOn;
    }

    /**
     * Gets the nickname of this credential.
     * @return A String
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Gets the date this credential was enrolled on (milliseconds since January 1, 1970, 00:00:00 GMT)
     * @return A long value
     */
    public long getAddedOn() {
        return addedOn;
    }

}
