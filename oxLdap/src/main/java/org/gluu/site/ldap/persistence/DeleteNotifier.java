/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.site.ldap.persistence;

public interface DeleteNotifier {
	public void onBeforeRemove(String dn);

	public void onAfterRemove(String dn);

}
