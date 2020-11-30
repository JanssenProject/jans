/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.persistence.model.configuration;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.as.persistence.model.base.Entry;

/**
 * Provides global inum search ability.
 * @author Oleksiy Tataryn
 *
 */
@DataEntry
@ObjectClass
public class InumEntry extends Entry {

	@AttributeName(ignoreDuringUpdate = true)
	private String inum;

	/**
	 * @param inum the inum to set
	 */
	public void setInum(String inum) {
		this.inum = inum;
	}


	/**
	 * @return the inum
	 */
	public String getInum() {
		return inum;
	}


	@Override
	public String toString() {
		return String.format("Entry [dn=%s, inum=%s]", getDn(), getInum());
	}


}
