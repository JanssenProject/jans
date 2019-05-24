/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2019, Gluu
 */

package org.gluu.radius.model;

import java.io.Serializable;


import org.gluu.persist.annotation.AttributeName;
import org.gluu.persist.annotation.DataEntry;
import org.gluu.persist.annotation.DN;
import org.gluu.persist.annotation.ObjectClass;


@DataEntry
@ObjectClass(values={"oxRadiusClient","top"})
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

	public RadiusClient setDn(String dn) {

		this.dn = dn;
		return this;
	}


	public String getInum() {

		return this.inum;
	}

	public RadiusClient setInum(String inum) {

		this.inum = inum;
		return this;
	}


	public String getName() {

		return this.name;
	}

	public RadiusClient setName(String name) {

		this.name = name;
		return this;
	}


	public String getIpAddress() {

		return this.ipAddress;
	}

	public RadiusClient setIpAddress(String ipaddress) {

		this.ipAddress = ipaddress;
		return this;
	}


	public String getSecret() {

		return this.secret;
	}

	public RadiusClient setSecret(String secret) {

		this.secret = secret;
		return this;
	}

	public Integer getPriority() {

		return this.priority;
	}

	public RadiusClient setPriority(Integer priority) {

		this.priority = priority;
		return this;
	}
}