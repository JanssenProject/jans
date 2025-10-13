package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuditLogConf implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Schema(description = "Flag to enable and disable audit log.")
    private boolean enabled;
    
    @Schema(description = "Flag to enable and disable loggin g data.")
    private boolean logData;

    @Schema(description = "HTTP methods for which audit is disabled.")
    private List<String> ignoreHttpMethod;
	
	@Schema(description = "Annotation for which audit is disabled.")
    private List<String> ignoreAnnotation;
	
    @Schema(description = "Object for which audit is disabled.")
    private List<ObjectDetails> ignoreObjectMapping;

    @Schema(description = "List of header HTTP attributes whose value is to be logged.")
    private List<String> headerAttributes;

    @Schema(description = "Audit log file location.")
    private String auditLogFilePath;

    @Schema(description = "Audit log file name.")
    private String auditLogFileName;
    
    @Schema(description = "Date format in audit log file.")
    private String auditLogDateFormat;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogData() {
        return logData;
    }

    public void setLogData(boolean logData) {
        this.logData = logData;
    }

    public List<String> getIgnoreHttpMethod() {
        return ignoreHttpMethod;
    }

    public void setIgnoreHttpMethod(List<String> ignoreHttpMethod) {
        this.ignoreHttpMethod = ignoreHttpMethod;
    }

    public List<String> getIgnoreAnnotation() {
        return ignoreAnnotation;
    }

    public void setIgnoreAnnotation(List<String> ignoreAnnotation) {
        this.ignoreAnnotation = ignoreAnnotation;
    }

    public List<ObjectDetails> getIgnoreObjectMapping() {
        return ignoreObjectMapping;
    }

    public void setIgnoreObjectMapping(List<ObjectDetails> ignoreObjectMapping) {
        this.ignoreObjectMapping = ignoreObjectMapping;
    }

    public List<String> getHeaderAttributes() {
        return headerAttributes;
    }

    public void setHeaderAttributes(List<String> headerAttributes) {
        this.headerAttributes = headerAttributes;
    }

    public String getAuditLogFilePath() {
        return auditLogFilePath;
    }

    public void setAuditLogFilePath(String auditLogFilePath) {
        this.auditLogFilePath = auditLogFilePath;
    }

    public String getAuditLogFileName() {
        return auditLogFileName;
    }

    public void setAuditLogFileName(String auditLogFileName) {
        this.auditLogFileName = auditLogFileName;
    }

    public String getAuditLogDateFormat() {
        return auditLogDateFormat;
    }

    public void setAuditLogDateFormat(String auditLogDateFormat) {
        this.auditLogDateFormat = auditLogDateFormat;
    }

    @Override
    public String toString() {
        return "AuditLogConf [enabled=" + enabled + ", logData=" + logData + ", ignoreHttpMethod=" + ignoreHttpMethod
                + ", ignoreAnnotation=" + ignoreAnnotation + ", ignoreObjectMapping=" + ignoreObjectMapping
                + ", headerAttributes=" + headerAttributes + ", auditLogFilePath=" + auditLogFilePath
                + ", auditLogFileName=" + auditLogFileName + ", auditLogDateFormat=" + auditLogDateFormat + "]";
    }
    
}
