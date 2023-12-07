/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2023, Janssen Project
 */

package io.jans.service.message.model.config;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author Yuriy Movchan Date: 30/11/2023
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostgresMessageConfiguration implements Serializable {

	@XmlElement(name = "db.schema.name")
	private String dbSchemaName;

	@XmlElement(name = "connection.uri")
	private String connectionUri;

	@XmlElement(name = "auth.userName")
	private String authUserName;

	@XmlElement(name = "auth.userPassword")
	private String authUserPassword;

	@XmlElement(name = "connection.pool.max-total")
	private Integer connectionPoolMaxTotal;

	@XmlElement(name = "connection.pool.max-idle")
	private Integer connectionPoolMaxIdle;

	@XmlElement(name = "connection.pool.min-idle")
	private Integer connectionPoolMinIdle;

	@XmlElement(name = "message.wait-millis")
	private Integer messageWaitMillis;

	@XmlElement(name = "message.sleep-thread-millis")
	private Integer messageSleepThreadTime;

	public String getDbSchemaName() {
		return dbSchemaName;
	}

	public void setDbSchemaName(String dbSchemaName) {
		this.dbSchemaName = dbSchemaName;
	}

	public String getConnectionUri() {
		return connectionUri;
	}

	public void setConnectionUri(String connectionUri) {
		this.connectionUri = connectionUri;
	}

	public String getAuthUserName() {
		return authUserName;
	}

	public void setAuthUserName(String authUserName) {
		this.authUserName = authUserName;
	}

	public String getAuthUserPassword() {
		return authUserPassword;
	}

	public void setAuthUserPassword(String authUserPassword) {
		this.authUserPassword = authUserPassword;
	}

	public Integer getConnectionPoolMaxTotal() {
		return connectionPoolMaxTotal;
	}

	public void setConnectionPoolMaxTotal(Integer connectionPoolMaxTotal) {
		this.connectionPoolMaxTotal = connectionPoolMaxTotal;
	}

	public Integer getConnectionPoolMaxIdle() {
		return connectionPoolMaxIdle;
	}

	public void setConnectionPoolMaxIdle(Integer connectionPoolMaxIdle) {
		this.connectionPoolMaxIdle = connectionPoolMaxIdle;
	}

	public Integer getConnectionPoolMinIdle() {
		return connectionPoolMinIdle;
	}

	public void setConnectionPoolMinIdle(Integer connectionPoolMinIdle) {
		this.connectionPoolMinIdle = connectionPoolMinIdle;
	}

	public Integer getMessageWaitMillis() {
		return messageWaitMillis;
	}

	public void setMessageWaitMillis(Integer messageWaitMillis) {
		this.messageWaitMillis = messageWaitMillis;
	}

	public Integer getMessageSleepThreadTime() {
		return messageSleepThreadTime;
	}

	public void setMessageSleepThreadTime(Integer messageSleepThreadTime) {
		this.messageSleepThreadTime = messageSleepThreadTime;
	}

	@Override
	public String toString() {
		return "PostgresMessageConfiguration [dbSchemaName=" + dbSchemaName + ", connectionUri=" + connectionUri
				+ ", authUserName=" + authUserName + ", authUserPassword=" + authUserPassword
				+ ", connectionPoolMaxTotal=" + connectionPoolMaxTotal + ", connectionPoolMaxIdle="
				+ connectionPoolMaxIdle + ", connectionPoolMinIdle=" + connectionPoolMinIdle + ", messageWaitMillis="
				+ messageWaitMillis + ", messageSleepThreadTime=" + messageSleepThreadTime + "]";
	}

}
