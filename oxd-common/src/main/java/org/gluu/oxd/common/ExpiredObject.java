package org.gluu.oxd.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

public class ExpiredObject {
    private String key;
    private String value;
    private Long createdAt;
    private Long expiredAt;
    private ExpiredObjectType type;

    private static final Logger LOG = LoggerFactory.getLogger(ExpiredObject.class);

    public ExpiredObject() {
    }

    public ExpiredObject(String key, ExpiredObjectType expiredObjectType, int expiredObjectExpirationInMins) {
        Preconditions.checkState(!Strings.isNullOrEmpty(key), "Expired Object contains blank or null value. Please specify valid Expired Object.");

        Calendar cal = Calendar.getInstance();

        this.key = key;
        this.type = expiredObjectType;
        this.createdAt = cal.getTimeInMillis();
        cal.add(Calendar.MINUTE, expiredObjectExpirationInMins);
        this.expiredAt = cal.getTimeInMillis();
        try {
            this.value = Jackson2.createJsonMapperWithoutEmptyAttributes().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.error("Error in assigning json value to ExpiredObject value attribute.", e);
        }
    }

    public ExpiredObject(String key, ExpiredObjectType expiredObjectType, Long createdAt, Long expiredAt) {
        Preconditions.checkState(!Strings.isNullOrEmpty(key), "Expired Object contains blank or null value. Please specify valid Expired Object.");

        Calendar cal = Calendar.getInstance();

        this.key = key;
        this.type = expiredObjectType;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        try {
            this.value = Jackson2.createJsonMapperWithoutEmptyAttributes().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.error("Error in assigning json value to ExpiredObject value attribute.", e);
        }
    }

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

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Long expiredAt) {
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
