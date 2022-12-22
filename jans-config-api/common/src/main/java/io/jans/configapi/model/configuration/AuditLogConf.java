package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogConf {

    /**
     * Flag to enable and disable audit log
     */
    private boolean enabled;

    /**
     * List of header attributes
     */
    private List<String> headerAttributes;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getHeaderAttributes() {
        return headerAttributes;
    }

    public void setHeaderAttributes(List<String> headerAttributes) {
        this.headerAttributes = headerAttributes;
    }

    @Override
    public String toString() {
        return "AuditLogConf [enabled=" + enabled + ", headerAttributes=" + headerAttributes + "]";
    }

}
