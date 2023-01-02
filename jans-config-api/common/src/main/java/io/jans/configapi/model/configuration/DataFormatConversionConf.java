package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DataFormatConversionConf {

    /**
     * Flag to enable and disable data conversion
     */
    private boolean enabled;   

    @Override
    public String toString() {
        return "AuditLogConf [enabled=" + enabled + "]";
    }

}
