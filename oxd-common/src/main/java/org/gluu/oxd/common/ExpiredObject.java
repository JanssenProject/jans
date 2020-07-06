package org.gluu.oxd.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.gluu.persist.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

@DataEntry
@ObjectClass("oxExpiredObject")
public class ExpiredObject implements Serializable {

    @DN
    private String dn;
    @AttributeName(name = "oxId")
    private String key;
    @AttributeName(name = "dat")
    private String value;
    @AttributeName(name = "iat")
    private Date iat;
    @AttributeName(name = "exp")
    private Date exp;
    @AttributeName(name = "oxType")
    private String typeString;
    private ExpiredObjectType type;
    @Expiration
    private Integer ttl; // in seconds

    private static final Logger LOG = LoggerFactory.getLogger(ExpiredObject.class);

    public ExpiredObject() {
    }

    public ExpiredObject(String key, String value, ExpiredObjectType expiredObjectType, int expiredObjectExpirationInMins) {
        Preconditions.checkState(!Strings.isNullOrEmpty(key), "Expired Object contains blank or null key. Please specify valid Expired Object.");
        Preconditions.checkState(!Strings.isNullOrEmpty(value), "Expired Object contains blank or null value. Please specify valid Expired Object.");

        Calendar cal = Calendar.getInstance();

        this.key = key;
        this.type = expiredObjectType;
        this.typeString = expiredObjectType.getValue();
        this.iat = cal.getTime();
        cal.add(Calendar.MINUTE, expiredObjectExpirationInMins);
        this.exp = cal.getTime();
        this.ttl = expiredObjectExpirationInMins * 60;
        this.value = value;
    }

    public ExpiredObject(String key, String value, ExpiredObjectType expiredObjectType, Date iat, Date exp) {
        Preconditions.checkState(!Strings.isNullOrEmpty(key), "Expired Object contains blank or null value. Please specify valid Expired Object.");
        Preconditions.checkState(!Strings.isNullOrEmpty(value), "Expired Object contains blank or null value. Please specify valid Expired Object.");
        this.key = key;
        this.type = expiredObjectType;
        this.typeString = expiredObjectType.getValue();
        this.iat = iat;
        this.exp = exp;
        this.value = value;
    }

    public String getDn() {
        return this.dn;
    }

    public void setDn(String dn) {
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

    public ExpiredObjectType getType() {
        return type;
    }

    public void setType(ExpiredObjectType type) {
        this.type = type;
    }

    public String getTypeString() {
        return this.typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public Date getIat() {
        return iat;
    }

    public void setIat(Date iat) {
        this.iat = iat;
    }

    public Date getExp() {
        return exp;
    }

    public void setExp(Date exp) {
        this.exp = exp;
    }

    @Override
    public String toString() {
        return "ExpiredObject{" +
                "dn='" + dn + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", iat=" + iat +
                ", exp=" + exp +
                ", type=" + type.getValue() +
                ", ttl=" + ttl +
                '}';
    }

}
