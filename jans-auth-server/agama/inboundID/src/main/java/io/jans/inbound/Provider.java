package io.jans.inbound;

public class Provider {
    
    private String flowQname;
    private String displayName;
    private String logoImg;
    private String mappingClassField;

    private boolean enabled = true;
    private boolean skipProfileUpdate;
    private boolean cumulativeUpdate;
    private boolean requestForEmail;
    private boolean emailLinkingSafe;

    public String getFlowQname() {
        return flowQname;
    }

    public void setFlowQname(String flowQname) {
        this.flowQname = flowQname;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getMappingClassField() {
        return mappingClassField;
    }
    
    public void setMappingClassField(String mappingClassField) {
        this.mappingClassField = mappingClassField;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isSkipProfileUpdate() {
        return skipProfileUpdate;
    }
    
    public void setSkipProfileUpdate(boolean skipProfileUpdate) {
        this.skipProfileUpdate = skipProfileUpdate;
    }

    public boolean isCumulativeUpdate() {
        return cumulativeUpdate;
    }
    
    public void setCumulativeUpdate(boolean cumulativeUpdate) {
        this.cumulativeUpdate = cumulativeUpdate;
    }
    
    public String getLogoImg() {
        return logoImg;
    }

    public void setLogoImg(String logoImg) {
        this.logoImg = logoImg;
    }

    public boolean isRequestForEmail() {
        return requestForEmail;
    }

    public void setRequestForEmail(boolean requestForEmail) {
        this.requestForEmail = requestForEmail;
    }

    public boolean isEmailLinkingSafe() {
        return emailLinkingSafe;
    }

    public void setEmailLinkingSafe(boolean emailLinkingSafe) {
        this.emailLinkingSafe = emailLinkingSafe;
    }

}
