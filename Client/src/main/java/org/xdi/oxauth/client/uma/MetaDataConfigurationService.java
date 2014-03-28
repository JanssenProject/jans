package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the requester can obtain UMA metadata configuration.
 * 
 * @author Yuriy Movchan Date: 10/25/2012
 */
public interface MetaDataConfigurationService {

	@GET
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public MetadataConfiguration getMetadataConfiguration();

}