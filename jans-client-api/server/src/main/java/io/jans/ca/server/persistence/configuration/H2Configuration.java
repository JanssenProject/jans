package io.jans.ca.server.persistence.configuration;

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

    public static final String DEFAULT_DB_FILE_LOCATION_LINUX = "/opt/jans_client_api/data/rp_db";
    public static final String DEFAULT_DB_FILE_LOCATION_WINDOWS = "C:\\opt\\jans_client_api\\data\\rp_db";

    private String dbFileLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_DB_FILE_LOCATION_LINUX : DEFAULT_DB_FILE_LOCATION_WINDOWS;
    private String username;
    private String password;

    public String getDbFileLocation() {
        return dbFileLocation;
    }

    public void setDbFileLocation(String dbFileLocation) {
        this.dbFileLocation = dbFileLocation;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "H2Configuration{" +
                "dbFileLocation='" + dbFileLocation + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
