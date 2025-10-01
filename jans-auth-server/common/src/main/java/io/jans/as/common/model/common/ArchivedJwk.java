package io.jans.as.common.model.common;

import io.jans.orm.annotation.*;
import io.jans.orm.model.base.DeletableEntity;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Yuriy Z
 */
@DataEntry
@ObjectClass(value = "jansArchJwk")
public class ArchivedJwk extends DeletableEntity implements Serializable {

    @DN
    private String dn;

    @AttributeName(name = "jansId")
    private String id;

    @AttributeName(name = "creationDate")
    private Date creationDate = new Date();

    @JsonObject
    @AttributeName(name = "jansData")
    private JSONObject data;

    @AttributeName(name = "attr")
    @JsonObject
    private ArchivedKeyAttributes attributes;

    @Expiration
    private int ttl;

    @Override
    public String getDn() {
        return dn;
    }

    @Override
    public void setDn(String dn) {
        this.dn = dn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public JSONObject getData() {
        return data;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public ArchivedKeyAttributes getAttributes() {
        if (attributes == null) attributes = new ArchivedKeyAttributes();
        return attributes;
    }

    public void setAttributes(ArchivedKeyAttributes attributes) {
        this.attributes = attributes;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "ArchivedKey{" +
                "dn='" + dn + '\'' +
                ", id='" + id + '\'' +
                ", creationDate=" + creationDate +
                ", data=" + data +
                ", attributes=" + attributes +
                ", ttl=" + ttl +
                "} " + super.toString();
    }
}
