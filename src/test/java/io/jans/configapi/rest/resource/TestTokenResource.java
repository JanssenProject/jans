package io.jans.configapi.rest.resource;

import com.couchbase.client.core.message.ResponseStatus;
import io.jans.as.persistence.model.configuration.GluuConfiguration;
import io.jans.configapi.util.ApiTestMode;
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


@Path("/test" + "/token")
public class TestTokenResource extends BaseResource {
    
    @Inject
    Logger log;

    @Inject
    ApiTestMode apiTestMode;
    
    
    @GET
    public String createTestToken(@QueryParam("method") String method, @QueryParam("path") String path) throws Exception {
        System.out.println("\n\n\n ************* TestTokenResource:::createTestToken() - Entry - method = "+method+" ,path = "+path+"************* ");
        String token = this.apiTestMode.createTestToken(method,path);
        System.out.println("\n\n\n ************* TestTokenResource:::createTestToken() - token ="+token+"************* ");
        return token;
        
    }
}
