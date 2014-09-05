/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * ## Claims Mapping, the format is: <LDAP_VALUE>: <OIC_CLAIM>
 * uid: sub
 * displayName: name
 * givenName: given_name
 * sn: family_name
 * #<LDAP_VALUE>: middle_name
 * #<LDAP_VALUE>: nickname
 * #<LDAP_VALUE>: preferred_username
 * #<LDAP_VALUE>: profile
 * photo1: picture
 * #<LDAP_VALUE>: website
 * #<LDAP_VALUE>: gender
 * #<LDAP_VALUE>: birthdate
 * timezone: zoneinfo
 * preferredLanguage: locale
 * #<LDAP_VALUE>: updated_time
 * mail: email
 * #<LDAP_VALUE>: email_verified
 * homePostalAddress: formatted
 * street: street_address
 * l: locality
 * st: region
 * postalCode: postal_code
 * #<LDAP_VALUE>: country
 * telephoneNumber: phone_number
 * oxInum: inum
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/01/2013
 */
@XmlRootElement(name = "claim-mapping")
public class ClaimMappingConfiguration {
    @XmlAttribute(name = "ldap")
    private String ldap;
    @XmlAttribute(name = "claim")
    private String claim;

    public ClaimMappingConfiguration() {
    }

    public ClaimMappingConfiguration(String p_ldap, String p_claim) {
        ldap = p_ldap;
        claim = p_claim;
    }

    public static List<ClaimMappingConfiguration> liveInstance() {
        return ConfigurationFactory.getClaimMappings();
    }

    public static ClaimMappingConfiguration getMappingByLdap(String p_ldapName) {
        final List<ClaimMappingConfiguration> list = liveInstance();
        if (list != null && !list.isEmpty()) {
            for (ClaimMappingConfiguration m : list) {
                if (m.getLdap().equals(p_ldapName)) {
                    return m;
                }
            }
        }
        return null;
    }

    public static ClaimMappingConfiguration getMappingByClaim(String p_claimName) {
        final List<ClaimMappingConfiguration> list = liveInstance();
        if (list != null && !list.isEmpty()) {
            for (ClaimMappingConfiguration m : list) {
                if (m.getClaim().equals(p_claimName)) {
                    return m;
                }
            }
        }
        return null;
    }

    public static String getClaimByLdap(String p_ldapName) {
        final ClaimMappingConfiguration m = getMappingByLdap(p_ldapName);
        return m != null ? m.getClaim() : "";
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String p_claim) {
        claim = p_claim;
    }

    public String getLdap() {
        return ldap;
    }

    public void setLdap(String p_ldap) {
        ldap = p_ldap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClaimMappingConfiguration that = (ClaimMappingConfiguration) o;

        if (claim != null ? !claim.equals(that.claim) : that.claim != null) return false;
        if (ldap != null ? !ldap.equals(that.ldap) : that.ldap != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ldap != null ? ldap.hashCode() : 0;
        result = 31 * result + (claim != null ? claim.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ClaimMappingConfiguration");
        sb.append("{claim='").append(claim).append('\'');
        sb.append(", ldap='").append(ldap).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
