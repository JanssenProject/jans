package io.jans.casa.plugins.certauthn.model;

public class Reference {

    private String userId;
    private boolean enroll;
    private long expiresAt;
    
    public Reference() {}
    
    public Reference(String userId, boolean enroll, long expiresAt) {
        setUserId(userId);
        setEnroll(enroll);
        setExpiresAt(expiresAt);
    }
    
    public String getUserId() {
        return userId;
    }
    
    public boolean isEnroll() {
        return enroll;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setEnroll(boolean enroll) {
        this.enroll = enroll;
    }
    
    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
    
}
