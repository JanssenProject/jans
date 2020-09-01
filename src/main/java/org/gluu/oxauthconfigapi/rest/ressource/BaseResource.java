/**
 * 
 */
package org.gluu.oxauthconfigapi.rest.ressource;

import javax.ws.rs.core.Response;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class BaseResource {
	protected static final String READ_ACCESS = "oxauth-config-read";
	protected static final String WRITE_ACCESS = "oxauth-config-write";

	/**
	 * @return
	 */
	Response getResourceNotFoundError() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param e
	 * @return
	 */
	Response getInternalServerError(Exception e) {
		// TODO Auto-generated method stub
		return null;
	}

	Response getMissingAttributeError(String e) {
		// TODO Auto-generated method stub
		return null;
	}

	Response getResourceNotFoundError(Exception e) {
		// TODO Auto-generated method stub
		return null;
	}

	Response getResourceNotFoundError(String e) {
		// TODO Auto-generated method stub
		return null;
	}

}
