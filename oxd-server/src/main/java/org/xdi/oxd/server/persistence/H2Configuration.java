package org.xdi.oxd.server.persistence;

import org.apache.commons.lang.SystemUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class H2Configuration implements Serializable {

    public static final String DEFAULT_DB_FILE_LOCATION_LINUX = "/opt/oxd-server/bin/oxd_db";
    public static final String DEFAULT_DB_FILE_LOCATION_WINDOWS = "C:\\opt\\oxd-server\\bin\\oxd_db";

    private String dbFileLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_DB_FILE_LOCATION_LINUX : DEFAULT_DB_FILE_LOCATION_WINDOWS;

    public String getDbFileLocation() {
        return dbFileLocation;
    }

    public void setDbFileLocation(String dbFileLocation) {
        this.dbFileLocation = dbFileLocation;
    }

    @Override
    public String toString() {
        return "H2Configuration{" +
                "dbFileLocation='" + dbFileLocation + '\'' +
                '}';
    }
}
