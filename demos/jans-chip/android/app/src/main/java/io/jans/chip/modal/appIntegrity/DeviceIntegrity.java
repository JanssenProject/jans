package io.jans.chip.modal.appIntegrity;

import java.util.List;

public class DeviceIntegrity{
    private List<String> deviceRecognitionVerdict;

    public List<String> getDeviceRecognitionVerdict() {
        return deviceRecognitionVerdict;
    }

    public void setDeviceRecognitionVerdict(List<String> deviceRecognitionVerdict) {
        this.deviceRecognitionVerdict = deviceRecognitionVerdict;
    }

    public String commasSeparatedString(){
        if(this.deviceRecognitionVerdict != null) {
            return String.join(",", this.deviceRecognitionVerdict);
        }
        return null;
    }
}
