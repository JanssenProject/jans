package org.xdi.oxauth.service;

import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.ui.ClientUidSelector;
import org.jboss.seam.util.RandomStringUtils;

/**
 * Fix cookie value in Seam component
 * 
 * @author Yuriy Movchan Date: 07/10/2013
 */
@Name("org.jboss.seam.ui.clientUidSelector")
@Install(precedence = Install.DEPLOYMENT)
public class FixedClientUidSelector extends ClientUidSelector {

	private static final long serialVersionUID = -7004476980453250712L;

	private String clientUid;

	@Create
	public void onCreate() {
		setCookiePath(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath());
		setCookieMaxAge(-1);
		setCookieEnabled(true);
		clientUid = getCookieValue();
	}

	public void seed() {
		if (!isSet()) {
			clientUid = RandomStringUtils.random(50, true, true); // Fix
			setCookieValueIfEnabled(clientUid);
		}
	}

	public boolean isSet() {
		return clientUid != null;
	}

	public String getClientUid() {
		return clientUid;
	}

	@Override
	protected String getCookieName() {
		return "javax.faces.ClientToken";
	}

}