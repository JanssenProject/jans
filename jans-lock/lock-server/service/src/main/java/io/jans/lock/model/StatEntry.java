package io.jans.lock.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
@DataEntry
@ObjectClass(value = "jansLockStatEntry")
public class StatEntry extends BaseEntry {

	private static final long serialVersionUID = 7349181838267756343L;

	@AttributeName(name = "jansId")
	private String id;

	@AttributeName(name = "jansData")
	private String month;

	@AttributeName(name = "dat")
	private String userHllData;

	@AttributeName(name = "clntDat")
	private String clientHllData;

	@AttributeName(name = "attr")
	@JsonObject
	private Stat stat;

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserHllData() {
		return userHllData;
	}

	public void setUserHllData(String userHllData) {
		this.userHllData = userHllData;
	}

	public String getClientHllData() {
		return clientHllData;
	}

	public void setClientHllData(String clientHllData) {
		this.clientHllData = clientHllData;
	}

	public Stat getStat() {
		if (stat == null) {
			stat = new Stat();
		}
		return stat;
	}

	public void setStat(Stat stat) {
		this.stat = stat;
	}
}
