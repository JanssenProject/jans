package io.jans.casa.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a registered credential of OTP type (verifiedMobile is not considered OTP device in this application)
 * @author jgomer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OTPDevice extends RegisteredCredential implements Comparable<OTPDevice> {

    private long addedOn;

    private int id;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    //Primite boolean not used so that we can cope with cases where soft is missing (cred-manager installations)
    private Boolean soft;

    @JsonIgnore
    private String uid;

    @JsonIgnore
    private boolean timeBased;

    public OTPDevice() {
    }

    public OTPDevice(String uid) {
        this.uid = uid;
        updateHash();
        timeBased = uid.startsWith("totp:");
    }

    public OTPDevice(int id) {
        this.id = id;
    }

    public long getAddedOn() {
        return addedOn;
    }

    public int getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public boolean isTimeBased() {
        return timeBased;
    }

    public Boolean getSoft() {
        return soft;
    }

    public void setAddedOn(long addedOn) {
        this.addedOn = addedOn;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUid(String uid) {
        this.uid = uid;
        updateHash();
    }

    public void setTimeBased(boolean timeBased) {
        this.timeBased = timeBased;
    }

    public void setSoft(Boolean soft) {
        this.soft = soft;
    }
    
    public String getKey() {
        
        String str = uid.replaceFirst("hotp:", "").replaceFirst("totp:", "");
        int idx = str.lastIndexOf(";");
        if (idx > 0) {
            str = str.substring(0, idx);
        }
        return str;
        
    }

    public int compareTo(OTPDevice d2) {
        long date1 = getAddedOn();
        long date2 = d2.getAddedOn();
        return date1 < date2 ? -1 : ((date1 > date2) ? 1 : 0);
    }

    public int currentMovingFactor() {       
        return timeBased ? -1 : Integer.valueOf(uid.substring(uid.lastIndexOf(";") + 1));        
    }
    
    private void updateHash() {
        id = uid == null ? 0 : getKey().hashCode();
    }

}
