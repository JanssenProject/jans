package org.gluu.oxd.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.Calendar;
import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

@DataEntry
@ObjectClass("oxExpiredObject")
public class ExpiredObject implements Serializable {

    @DN
    private String dn;
    @AttributeName(name="key")
    private String key;
    @AttributeName(name="value")
    private String value;
    @AttributeName(name="createdAt")
    private Long createdAt;
    @AttributeName(name="expiredAt")
    private Long expiredAt;
    @AttributeName(name="type")
    private String typeString;
    private ExpiredObjectType type;

    private static final Logger LOG = LoggerFactory.getLogger(ExpiredObject.class);

    public ExpiredObject() {
    }

    public ExpiredObject(String key, ExpiredObjectType expiredObjectType, int expiredObjectExpirationInMins) {
        Preconditions.checkState(!Strings.isNullOrEmpty(key), "Expired Object contains blank or null value. Please specify valid Expired Object.");

        Calendar cal = Calendar.getInstance();

        this.key = key;
        this.type = expiredObjectType;
        this.typeString = expiredObjectType.getValue();
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
        this.typeString = expiredObjectType.getValue();
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        try {
            this.value = Jackson2.createJsonMapperWithoutEmptyAttributes().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOG.error("Error in assigning json value to ExpiredObject value attribute.", e);
        }
    }

    public String getDn()
    {
        return this.dn;
    }

    public void setDn(String dn)
    {
        this.dn = dn;
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

    public String getTypeString()
    {
        return this.typeString;
    }

    public void setTypeString(String typeString)
    {
        this.typeString = typeString;
    }

    @Override
    public String toString() {
        return "ExpiredObject{" +
                "dn='" + dn + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", createdAt=" + createdAt +
                ", expiredAt=" + expiredAt +
                ", type=" + type.getValue() +
                '}';
    }

}
