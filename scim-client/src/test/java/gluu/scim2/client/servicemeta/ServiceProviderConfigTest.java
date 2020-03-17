package gluu.scim2.client.servicemeta;

import gluu.scim2.client.BaseTest;
import org.gluu.oxtrust.model.scim2.annotations.Schema;
import org.gluu.oxtrust.model.scim2.provider.config.ServiceProviderConfig;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-10-21.
 */
public class ServiceProviderConfigTest extends BaseTest {

    private ServiceProviderConfig config;

    @BeforeTest
    public void init() throws Exception{
        Response response=client.getServiceProviderConfig();
        config=response.readEntity(ServiceProviderConfig.class);
    }

    @Test
    public void check(){
        String schema=ServiceProviderConfig.class.getAnnotation(Schema.class).id();
        assertEquals(schema, "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig");
        assertTrue(config.getSchemas().contains(schema));
        assertTrue(config.getAuthenticationSchemes().size()>0);
    }


}
