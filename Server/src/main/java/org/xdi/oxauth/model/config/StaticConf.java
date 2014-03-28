package org.xdi.oxauth.model.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/01/2013
 */
@XmlRootElement(name = "static")
@XmlAccessorType(XmlAccessType.FIELD)
public class StaticConf {

    @XmlElement(name = "claim-mapping")
    private List<ClaimMappingConfiguration> claimMapping;

    @XmlElement(name = "base-dn")
    private BaseDnConfiguration baseDn;

    public List<ClaimMappingConfiguration> getClaimMapping() {
        return claimMapping;
    }

    public void setClaimMapping(List<ClaimMappingConfiguration> p_claimMapping) {
        claimMapping = p_claimMapping;
    }

    public BaseDnConfiguration getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(BaseDnConfiguration p_baseDn) {
        baseDn = p_baseDn;
    }
}
