package org.xdi.oxauth.model.appliance;

import java.io.Serializable;
import java.util.List;

import org.gluu.site.ldap.persistence.annotation.LdapAttribute;
import org.gluu.site.ldap.persistence.annotation.LdapEntry;
import org.gluu.site.ldap.persistence.annotation.LdapObjectClass;

/**
 * Gluu Appliance
 *
 * @author Yuriy Movchan Date: 08.27.2012
 */
@LdapEntry
@LdapObjectClass(values = { "top", "gluuAppliance" })
public class GluuAppliance extends InumEntry implements Serializable {

	private static final long serialVersionUID = -2818003894646725601L;

	@LdapAttribute(ignoreDuringUpdate = true)
	private String inum;
	
	@LdapAttribute(name = "oxIDPAuthentication")
	private List<String> oxIDPAuthentication;
	
	public String getInum() {
		return inum;
	}

	public void setInum(String inum) {
		this.inum = inum;
	}

	public List<String> getOxIDPAuthentication(){
		return this.oxIDPAuthentication;
	}
	
	public void setOxIDPAuthentication(List<String> oxIDPAuthentication){
		this.oxIDPAuthentication = oxIDPAuthentication;
	}

	@Override
	public String toString() {
		return String.format("GluuAppliance [inum=%s, oxIDPAuthentication=%s, toString()=%s]", inum, oxIDPAuthentication, super.toString());
	}

}
