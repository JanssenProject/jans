package io.jans.configapi.plugin.shibboleth.model;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.io.Serializable;
import java.util.List;

@DataEntry
@ObjectClass(value = "jansAppConf")
public class ShibbolethIdpConfiguration extends Entry implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonObject
    @AttributeName(name = "jansConfApp")
    private ShibbolethIdpConfigurationProperties shibbolethIdpProperties;

    @AttributeName(name = "jansRevision")
    private long revision;

    public ShibbolethIdpConfigurationProperties getShibbolethIdpProperties() {
        return shibbolethIdpProperties;
    }

    public void setShibbolethIdpProperties(ShibbolethIdpConfigurationProperties shibbolethIdpProperties) {
        this.shibbolethIdpProperties = shibbolethIdpProperties;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    @Override
    public String toString() {
        return "ShibbolethIdpConfiguration{" +
                "shibbolethIdpProperties=" + shibbolethIdpProperties +
                ", revision=" + revision +
                '}';
    }
}
