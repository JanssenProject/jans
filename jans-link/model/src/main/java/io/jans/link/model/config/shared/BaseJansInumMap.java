/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.link.model.config.shared;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.io.Serializable;
import java.util.Arrays;

/**
 * GluuInumMap
 * 
 * @author Yuriy Movchan Date: 07.13.2011
 */
@DataEntry(sortBy = { "inum" })
@ObjectClass(value = "jansInumMap")
public class BaseJansInumMap extends Entry implements Serializable {

	private static final long serialVersionUID = -2190480357430436503L;

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	@AttributeName(name = "jansStatus")
	private GluuStatus status;

	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public GluuStatus getStatus() {
		return status;
	}

	public void setStatus(GluuStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GluuInumMap [inum=").append(inum).append(", status=").append(status).append("]");
		return builder.toString();
	}

}
