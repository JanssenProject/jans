package io.jans.as.persistence.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.DeletableEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * Pushed authorization request.
 *
 * @author Yuriy Zabrovarnyy
 */
@DataEntry
@ObjectClass(value = "jansPar")
public class Par extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = -3332496019942067971L;

    @AttributeName(name = "jansId", consistency = true)
    private String id;

    @AttributeName(name = "jansAttrs")
    @JsonObject
    private ParAttributes attributes;

    @Expiration
    private Integer ttl;

    public Par() {
        setDeletable(true);
    }

    public ParAttributes getAttributes() {
        if (attributes == null) attributes = new ParAttributes();
        return attributes;
    }

    public void setAttributes(ParAttributes attributes) {
        this.attributes = attributes;
    }

    public boolean isExpired() {
        return isExpired(new Date());
    }

    public boolean isExpired(Date now) {
        final Date exp = getExpirationDate();
        return exp == null || exp.before(now);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "Par{" +
                "dn='" + getDn() + '\'' +
                ", id='" + id + '\'' +
                ", attributes=" + attributes +
                "} " + super.toString();
    }
}
