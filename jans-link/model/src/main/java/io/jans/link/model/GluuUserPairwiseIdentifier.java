package io.jans.link.model;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

@DataEntry(sortBy = { "oxId" })
@ObjectClass(value = "pairwiseIdentifier")
public class GluuUserPairwiseIdentifier extends BaseEntry implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -449401585533639948L;

	@AttributeName(ignoreDuringUpdate = true)
	private String oxId;

	@AttributeName(ignoreDuringUpdate = true, name = "jansId")
	private String id;
	@AttributeName(name = "jansClntId")
	private String clientId;
	@AttributeName(name = "jansSectorIdentifier")
	private String sp;
	@AttributeName(name = "jansUsrId")
    private String userInum;

	public String getOxId() {
		return oxId;
	}

	public void setOxId(String oxId) {
		this.oxId = oxId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getSp() {
		return sp;
	}

	public void setSp(String sp) {
		this.sp = sp;
	}

	public String getUserInum() {
		return userInum;
	}

	public void setUserInum(String userInum) {
		this.userInum = userInum;
	}

	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj != null && getOxId() != null && obj instanceof GluuUserPairwiseIdentifier) {
			result = getOxId().equals(((GluuUserPairwiseIdentifier) obj).getOxId());
		}
		return result;
	}
}
