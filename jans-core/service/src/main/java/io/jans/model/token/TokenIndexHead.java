package io.jans.model.token;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;

import java.io.Serializable;

/**
 * @author Yuriy Z
 */
@DataEntry
@ObjectClass(value = "jansTknIndexHead")
public class TokenIndexHead implements Serializable {
    @DN
    private String dn;
    @AttributeName(name = "jansIndex", consistency = true)
    private Integer jansIndex;

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public Integer getJansIndex() {
        return jansIndex;
    }

    public void setJansIndex(Integer jansIndex) {
        this.jansIndex = jansIndex;
    }

    @Override
    public String toString() {
        return "TokenIndexHead{" +
                "dn='" + dn + '\'' +
                ", jansIndex=" + jansIndex +
                '}';
    }
}
