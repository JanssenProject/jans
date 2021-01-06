package io.jans.configapi.rest.resource;

import com.couchbase.client.core.message.ResponseStatus;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.util.TestUtil;
import io.jans.configapi.filters.ProtectedApi;
import io.jans.configapi.rest.model.AuthenticationMethod;
import io.jans.configapi.service.ConfigurationService;
import io.jans.configapi.util.ApiAccessConstants;
import io.jans.configapi.util.ApiConstants;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.validation.Valid;
import javax.ws.rs.QueryParam;


@Path("/test")
public class TestResource extends BaseResource {
    
    @Inject
    Logger log;

    @Inject
    TestUtil testUtil;
    
    
    @GET
    public String createTestToken(@QueryParam("method") String method, @QueryParam("path") String path) throws Exception {
       String token = this.testUtil.createTestToken(method,path);
       return token;        
    }
    
    @DELETE
    public void deleteTestClient() throws Exception {
       this.testUtil.deleteTestClient();
    }
}
