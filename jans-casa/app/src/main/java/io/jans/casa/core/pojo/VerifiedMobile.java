package io.jans.casa.core.pojo;

/**
 * Represents a registered credential corresponding to a verified mobile number
 * @author jgomer
 */
public class VerifiedMobile extends RegisteredCredential implements Comparable<VerifiedMobile> {

    private String number;

    private long addedOn;

    public VerifiedMobile() {
    }

    public VerifiedMobile(String number) {
        this.number = number;
    }

    public int compareTo(VerifiedMobile ph) {
        long date1 = getAddedOn();
        long date2 = ph.getAddedOn();
        return (date1 < date2) ? -1 : (date1 > date2 ? 1 : 0);
    }

    public long getAddedOn() {
        return addedOn;
    }

    public String getNumber() {
        return number;
    }

    public void setAddedOn(long addedOn) {
        this.addedOn = addedOn;
    }

    public void setNumber(String number) {
        this.number = number;
    }

}
