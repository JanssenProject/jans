/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.model;

/**
 * Database metadata
 *
 * @author Yuriy Movchan Date: 21/05/2025
 */
public class PersistenceMetadata {

	private final String databaseName;
	private final String schemaName;
	private final String productName;
	private final String productVersion;
	private final String driverName;
	private final String driverVersion;

	public PersistenceMetadata(String databaseName, String schemaName, String productName, String productVersion, String driverName, String driverVersion) {
		this.databaseName = databaseName;
		this.schemaName = schemaName;
		this.productName = productName;
		this.productVersion = productVersion;
		this.driverName = driverName;
		this.driverVersion = driverVersion;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getProductName() {
		return productName;
	}

	public String getProductVersion() {
		return productVersion;
	}

	public String getDriverName() {
		return driverName;
	}

	public String getDriverVersion() {
		return driverVersion;
	}

	@Override
	public String toString() {
		return "PersistenceMetadata [databaseName=" + databaseName + ", schemaName=" + schemaName + ", productName="
				+ productName + ", productVersion=" + productVersion + ", driverName=" + driverName + ", driverVersion="
				+ driverVersion + "]";
	}

}
