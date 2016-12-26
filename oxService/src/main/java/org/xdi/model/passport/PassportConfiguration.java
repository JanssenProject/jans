package org.xdi.model.passport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.gluu.site.ldap.persistence.annotation.LdapJsonObject;

/**
 * @author Shekhar L.
 * @Date 07/17/2016
 */


public class PassportConfiguration {
	
	private List <FieldSet> fieldset = new ArrayList <FieldSet>();

	public List <FieldSet> getFieldset() {
		return fieldset;
	}

	public void setFieldset(List <FieldSet> fieldset) {
		this.fieldset = fieldset;
	}
}
