/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
