package org.gluu.site.ldap.persistence;

public interface DeleteNotifier {
	public void onBeforeRemove(String dn);

	public void onAfterRemove(String dn);

}
