package org.gluu.oxd.common;

import java.sql.Timestamp;

public class ExpiredObject {
    private String key;
    private String value;
    private Timestamp createdAt;
    private Timestamp expiredAt;
    private ExpiredObjectType type;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Timestamp expiredAt) {
        this.expiredAt = expiredAt;
    }

    public ExpiredObjectType getType() {
        return type;
    }

    public void setType(ExpiredObjectType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ExpiredObject{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", createdAt=" + createdAt +
                ", expiredAt=" + expiredAt +
                ", type=" + type.getValue() +
                '}';
    }

}
