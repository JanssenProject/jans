package io.jans.as.persistence.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.Expiration;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.DeletableEntity;

import java.io.Serializable;

/**
 * Pushed authorization request.
 *
 * @author Yuriy Zabrovarnyy
 */
@ObjectClass(value = "jansPar")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Par extends DeletableEntity implements Serializable {

    private static final long serialVersionUID = -3332496019942067970L;

    @DN
    private String dn;

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

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "Par{" +
                "dn='" + dn + '\'' +
                ", id='" + id + '\'' +
                ", attributes=" + attributes +
                "} " + super.toString();
    }
}
