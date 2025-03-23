package io.jans.casa.core.pojo;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.jans.entry.Transports;

/**
 * Represents a fido registered credential
 */
public class FidoDevice extends RegisteredCredential implements Comparable<FidoDevice> {

    private String id;
    private long counter;
    private Date creationDate;
    private Date lastAccessTime;
    private String status;
    private String application;
    private String [] transports; 
    public String getId() {
        return id;
    }

    public long getCounter() {
        return counter;
    }

    public String getApplication() {
        return application;
    }

    public String getStatus() {
        return status;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public void setCounter(long counter) {
        this.counter = counter;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String [] getTransports()
    {
    	return transports;
    }
    
    public void setTransports(String t[])
    {
    	this.transports = t;
    }
    
    public int compareTo(FidoDevice k) {
        long date1 = getCreationDate().getTime();
        long date2 = k.getCreationDate().getTime();
        return (date1 < date2) ? -1 : ((date1 > date2) ? 1 : 0);
    }
    
    public static boolean isPlatformAuthenticator(FidoDevice device) {
        List<String> transports = Arrays.asList(device.getTransports());
        if(transports.contains(Transports.INTERNAL.getValue()) && transports.size() == 1)
            return true;
        return false;
    }
    public static boolean isMultideviceAuthenticator(FidoDevice device) {
    	List<String> transports = Arrays.asList(device.getTransports());
    	if(transports.contains(Transports.INTERNAL.getValue()) && transports.contains(Transports.HYBRID.getValue()))
            return true;
        return false;
    }
    public static boolean isSecurityKey(FidoDevice device) {
    	List<String> transports = Arrays.asList(device.getTransports());
    	if(transports.contains(Transports.USB.getValue()) || transports.contains(Transports.NFC.getValue()) || transports.contains(Transports.BLE.getValue()) )
            return true;
        return false;
    }

	@Override
	public String toString() {
		return "FidoDevice [id=" + id + ", counter=" + counter + ", creationDate=" + creationDate + ", lastAccessTime="
				+ lastAccessTime + ", status=" + status + ", application=" + application + ", transports="
				+ Arrays.toString(transports) + "]";
	}

}
