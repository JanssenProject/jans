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


	public RadiusClient() {

		this.dn = null;
		this.inum = null;
		this.name = null;
		this.ipAddress = null;
		this.secret = null;
	}

	public RadiusClient(String name,String ipaddress,String secret) {

		this.dn = null;
		this.inum = null;
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
}