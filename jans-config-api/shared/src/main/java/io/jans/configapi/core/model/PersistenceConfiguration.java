package io.jans.configapi.core.model;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

public class PersistenceConfiguration implements Serializable {
    
    private static final long serialVersionUID = -1214215449005176251L;
    
    @Schema(description = "Connection object's current catalog name.")
    private String databaseName; 
    
    @Schema(description = "Schema name.")
    private String schemaName; 
    
    @Schema(description = "Name of database product.")
    private String productName; 
    
    @Schema(description = "Version number of this database product.")
    private String productVersion;
    
    @Schema(description = "Name of this JDBC driver.")
    private String driverName; 
    
    @Schema(description = "JDBC driver version number.")
    private String driverVersion;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(String driverVersion) {
        this.driverVersion = driverVersion;
    }

    @Override
    public String toString() {
        return "PersistenceConfiguration [databaseName=" + databaseName + ", schemaName=" + schemaName
                + ", productName=" + productName + ", productVersion=" + productVersion + ", driverName=" + driverName
                + ", driverVersion=" + driverVersion + "]";
    }

 
}
