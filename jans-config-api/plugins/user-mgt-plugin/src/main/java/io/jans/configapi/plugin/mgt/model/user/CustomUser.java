package io.jans.configapi.plugin.mgt.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.as.common.model.common.User;
import io.jans.model.GluuStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomUser extends User {
 
    private static final long serialVersionUID = 1L;

    private String inum;
    private String mail;
    private String displayName;
    private GluuStatus jansStatus;
    private String givenName;
    private String userPassword;
    
        
    public String getInum() {
        return inum;
    }
    
    public void setInum(String inum) {
        this.inum = inum;
    }
    
    public String getMail() {
        return mail;
    }
    
    public void setMail(String mail) {
        this.mail = mail;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public GluuStatus getJansStatus() {
        return jansStatus;
    }
    
    public void setJansStatus(GluuStatus jansStatus) {
        this.jansStatus = jansStatus;
    }
    
    public String getGivenName() {
        return givenName;
    }
    
    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }
    
    public String getUserPassword() {
        return userPassword;
    }
    
    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
    
    @Override
    public String toString() {
        return "CustomUser [inum=" + inum + ", mail=" + mail + ", displayName=" + displayName + ", jansStatus="
                + jansStatus + ", givenName=" + givenName + ", userPassword=" + userPassword + "]";
    }
    
    
    
    
}
