package org.xdi.oxauth.util;

import java.net.URL;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests login examples in Weld
 */
//@RunWith(Arquillian.class) 
@RunAsClient
public class ArquillianTest extends Arquillian {

    protected String MAIN_PAGE = "/login";

    @ArquillianResource
    private URL contextPath;

    @Deployment(testable = false)
    public static WebArchive createTestDeployment1() {
        return Deployments.createDeployment();
    }

    @BeforeMethod
	public void openStartUrl() {
    	System.out.println(contextPath);
    }

    @Test
    public void loginTest() {
    	System.out.println(contextPath);
    }

    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void authenticateUser(
        @ArquillianResteasyResource final WebTarget webTarget)
    {
    	System.out.println(webTarget);
//    	webTarget.request().ge
    	
//    	tokenRestWebService.requestAccessToken(null, null, null, null, null, null, null, null, oxAuthExchangeToken, clientId, clientSecret, null, null, null)
//        final Response response = webTarget
//            .path("/sessions")
//            .request(MediaType.APPLICATION_JSON)
//            .get();
//        
//        System.out.println(response.getStatus());
    }
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/customer")
    public static interface CustomCustomerResource {

        /**
         * CustomerResource.getAllCustomers is annotated with<ul>
         * <li>Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})</li>
         * </ul>
         * By default proxy would use first mime type. We want returned response to be in JSON instead of XML.
         */
        @GET
        @Produces({MediaType.APPLICATION_JSON})
        List<String> getAllCustomers();

        /**
         * CustomerResource.createCustomer is annotated with<ul>
         * <li>Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})</li>
         * <li>Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})</li>
         * </ul>
         * By default proxy would use first mime type. We want returned response to be in XML instead of JSON.
         */
        @POST
        @Produces({MediaType.APPLICATION_XML})
        @Path("/")
        String createCustomer(Package pkg);
    }

}
