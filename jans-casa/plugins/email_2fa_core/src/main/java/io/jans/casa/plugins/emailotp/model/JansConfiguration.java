package io.jans.casa.plugins.emailotp.model;

import java.io.Serializable;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.InumEntry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JansConfiguration
 * 
 */
@DataEntry
@ObjectClass(value = "jansAppConf")
@JsonIgnoreProperties(ignoreUnknown = true)
public class JansConfiguration extends InumEntry implements Serializable {

    private static final long serialVersionUID = -1817003894646725601L;

    @AttributeName(name = "jansSmtpConf")
    @JsonObject
    private SmtpConfiguration smtpConfiguration;

    public final SmtpConfiguration getSmtpConfiguration() {
        return smtpConfiguration;
    }

    public final void setSmtpConfiguration(SmtpConfiguration smtpConfiguration) {
        this.smtpConfiguration = smtpConfiguration;
    }

    @Override
    public boolean equals(java.lang.Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        JansConfiguration gluuConfigurationObj = (JansConfiguration) obj;
        return smtpConfiguration.equals(gluuConfigurationObj.smtpConfiguration);
    }

    @Override
    public int hashCode() {
      return smtpConfiguration.hashCode();
    }
}
