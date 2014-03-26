package org.xdi.ldap.model;

import java.io.Serializable;

import org.gluu.site.ldap.persistence.annotation.LdapEntry;

/**
 * Person with custom attributes
 * 
 * @author Yuriy Movchan Date: 07.13.2011
 */
@LdapEntry
public class GluuDummyEntry extends Entry implements Serializable {

	private static final long serialVersionUID = -1111582184398161100L;

}
