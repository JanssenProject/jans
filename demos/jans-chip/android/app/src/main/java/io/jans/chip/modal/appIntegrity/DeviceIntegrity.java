package io.jans.chip.modal.appIntegrity;

import java.util.List;

public class DeviceIntegrity{
    private List<String> appRecognitionVerdict;

    public List<String> getAppRecognitionVerdict() {
        return appRecognitionVerdict;
    }

    public void setAppRecognitionVerdict(List<String> appRecognitionVerdict) {
        this.appRecognitionVerdict = appRecognitionVerdict;
    }

    public String commasSeparatedString(){
        if(this.appRecognitionVerdict != null) {
            return String.join(",", this.appRecognitionVerdict);
        }
        return null;
    }
}
