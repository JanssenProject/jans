/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.radius.model;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.ObjectClass;

@DataEntry
@ObjectClass(value = "jansRadiusClient")
public class RadiusClient implements Serializable {

	private static final long serialVersionUID = -3145075159422463151L;
	
	@DN
	private String dn;

	@AttributeName(name="inum")
	private String inum;

	@AttributeName(name="oxRadiusClientName")
	private String name;

	@AttributeName(name="oxRadiusClientIpAddress")
	private String ipAddress;

	@AttributeName(name="oxRadiusClientSecret")
	private String secret;

	@AttributeName(name="oxRadiusClientSortPriority")
	private Integer priority;


	public RadiusClient() {

	}

	public RadiusClient(String name,String ipaddress,String secret) {

		this.name = name;
		this.ipAddress = ipaddress;
		this.secret = secret;
	}


	public String getDn() {

		return this.dn;
	}

	public void setDn(String dn) {

		this.dn = dn;
	}


	public String getInum() {

		return this.inum;
	}

	public void setInum(String inum) {

		this.inum = inum;
	}


	public String getName() {

		return this.name;
	}

	public void setName(String name) {

		this.name = name;
	}


	public String getIpAddress() {

		return this.ipAddress;
	}

	public void setIpAddress(String ipaddress) {

		this.ipAddress = ipaddress;
	}


	public String getSecret() {

		return this.secret;
	}

	public void setSecret(String secret) {

		this.secret = secret;
	}

	public Integer getPriority() {

		return this.priority;
	}

	public void setPriority(Integer priority) {

		this.priority = priority;
	}
}