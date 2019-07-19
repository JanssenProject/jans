package org.gluu.oxd.server.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.SystemUtils;

import java.io.Serializable;

/**
 * @author yuriyz
 */
@JsonIgnoreProperties(
        ignoreUnknown = true
)
public class H2Configuration implements Serializable {

    public static final String DEFAULT_DB_FILE_LOCATION_LINUX = "/opt/oxd-server/data/oxd_db";
    public static final String DEFAULT_DB_FILE_LOCATION_WINDOWS = "C:\\opt\\oxd-server\\data\\oxd_db";

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
