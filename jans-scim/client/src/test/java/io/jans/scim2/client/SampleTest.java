package io.jans.scim2.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.scim.model.scim2.*;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.ws.rs.scim2.IUserWebService;
import io.jans.scim2.client.factory.ScimClientFactory;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-09-14.
 */
public class SampleTest extends BaseTest {

    private Logger logger = LogManager.getLogger(getClass());

    //This tests assumes client_secret_basic for token endpoint authentication
    @Test
    @Parameters ({"domainURL", "OIDCMetadataUrl", "clientId", "clientSecret"})
    public void smallerClient(String domainURL, String OIDCMetadataUrl, String clientId, 
    	String clientSecret) throws Exception {

        IUserWebService myclient = ScimClientFactory.getClient(IUserWebService.class, 
        	domainURL, OIDCMetadataUrl, clientId, clientSecret, false);

        SearchRequest sr=new SearchRequest();
        sr.setFilter("userName eq \"admin\"");

        Response response = myclient.searchUsersPost(sr);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource u = (UserResource) response.readEntity(ListResponse.class).getResources().get(0);
        logger.debug("Hello {}!", u.getDisplayName());

    }

}
