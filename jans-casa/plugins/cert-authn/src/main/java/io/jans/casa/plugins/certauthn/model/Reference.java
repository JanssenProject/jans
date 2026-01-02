package io.jans.casa.plugins.certauthn.model;

public class Reference {

    private String userId;
    private boolean enroll;
    
    public Reference() {}
    
    public Reference(String userId, boolean enroll) {
        setUserId(userId);
        setEnroll(enroll);
    }
    
    public String getUserId() {
        return userId;
    }
    
    public boolean isEnroll() {
        return enroll;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setEnroll(boolean enroll) {
        this.enroll = enroll;
    }

}
