/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.cache;

import io.jans.orm.annotation.*;
import io.jans.orm.model.base.Deletable;
import io.jans.orm.model.base.DeletableEntity;

import java.io.Serializable;
import java.util.Date;

@DataEntry
@ObjectClass(value = "jansCache")
public class NativePersistenceCacheEntity extends DeletableEntity implements Serializable, Deletable {

    @DN
    private String dn;
    @Expiration
    private Integer ttl;
    @AttributeName(name = "uuid")
    private String id;
    @AttributeName(name = "iat")
    private Date creationDate;
    @AttributeName(name = "dat")
    private String data;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

	public Integer getTtl() {
		return ttl;
	}

	public void setTtl(Integer ttl) {
		this.ttl = ttl;
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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
	public String toString() {
		return "NativePersistenceCacheEntity [dn=" + dn + ", ttl=" + ttl + ", id=" + id + ", creationDate=" + creationDate + ", data="
				+ data + "]";
	}
}
