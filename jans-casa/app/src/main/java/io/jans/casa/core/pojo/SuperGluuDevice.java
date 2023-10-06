package io.jans.casa.core.pojo;

import java.util.Date;

import io.jans.as.model.fido.u2f.protocol.DeviceData;
import io.jans.casa.core.model.Fido2RegistrationEntry;

/**
 * Represents a registered credential corresponding to a supergluu device
 */
public class SuperGluuDevice extends FidoDevice {

    private DeviceData deviceData;

    public SuperGluuDevice(Fido2RegistrationEntry entry) { 
    	
    	super.setId( entry.getId());
    	super.setCounter( entry.getCounter());
    	super.setCreationDate( entry.getCreationDate());
    	//this.lastAccessTime = entry.get;
    	super.setStatus ( entry.getRegistrationStatus());
    	super.setApplication ( entry.getApplication());
    	deviceData = entry.getDeviceData();
    }
    
    public SuperGluuDevice() { }

    public DeviceData getDeviceData() {
        return deviceData;
    }

    public void setDeviceData(DeviceData deviceData) {
        this.deviceData = deviceData;
    }

}
