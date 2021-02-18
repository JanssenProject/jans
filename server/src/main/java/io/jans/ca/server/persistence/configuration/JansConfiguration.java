package io.jans.ca.server.persistence.configuration;

public class JansConfiguration {

    private String baseDn;
    private String type;
    private String connection;
    private String salt;

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    @Override
    public String toString() {
        return "JansConfiguration{" +
                "baseDn='" + baseDn + '\'' +
                ", type='" + type + '\'' +
                ", connection='" + connection + '\'' +
                ", salt='" + salt + '\'' +
                '}';
    }
}
