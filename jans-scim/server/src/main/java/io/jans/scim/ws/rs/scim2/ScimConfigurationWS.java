package io.jans.scim.ws.rs.scim2;

import java.util.Collections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import io.jans.scim.ScimConfiguration;
import io.jans.service.JsonService;

/**
 * This class implements the endpoint at which the requester can obtain SCIM metadata configuration. Similar to the SCIM
 * /ServiceProviderConfig endpoint
 */
@ApplicationScoped
@Path("/scim-configuration")
@Api(value = "/.well-known/scim-configuration", description = "The SCIM server endpoint that provides configuration data.")
public class ScimConfigurationWS {

    @Inject
    private Logger log;

    @Inject
    private JsonService jsonService;

    @Inject
    private UserWebService userService;

    @Inject
    private GroupWebService groupService;

    @Inject
    private FidoDeviceWebService fidoService;

    @Inject
    private Fido2DeviceWebService fido2Service;

    @Inject
    private BulkWebService bulkService;

    @Inject
    private ServiceProviderConfigWS serviceProviderService;

    @Inject
    private ResourceTypeWS resourceTypeService;

    @Inject
    private SchemaWebService schemaService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Provides metadata as json document. It contains options and endpoints supported by the SCIM server.",
            response = ScimConfiguration.class
    )
    @ApiResponses(value = { @ApiResponse(code = 500, message = "Failed to build SCIM configuration json object.") })
    public Response getConfiguration() {

        try {
            final ScimConfiguration c2 = new ScimConfiguration();
            c2.setVersion("2.0");
            c2.setAuthorizationSupported(Collections.singletonList("oauth2"));
            c2.setUserEndpoint(userService.getEndpointUrl());
            c2.setGroupEndpoint(groupService.getEndpointUrl());
            c2.setFidoDevicesEndpoint(fidoService.getEndpointUrl());
            c2.setFido2DevicesEndpoint(fido2Service.getEndpointUrl());
            c2.setBulkEndpoint(bulkService.getEndpointUrl());
            c2.setServiceProviderEndpoint(serviceProviderService.getEndpointUrl());
            c2.setResourceTypesEndpoint(resourceTypeService.getEndpointUrl());
            c2.setSchemasEndpoint(schemaService.getEndpointUrl());

            // Convert manually to avoid possible conflicts between resteasy providers, e.g. jettison, jackson
            final String entity = jsonService.objectToPerttyJson(Collections.singletonList(c2));
            log.info("SCIM configuration: {}", entity);

            return Response.ok(entity).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to generate SCIM configuration").build());
        }
        
    }

}
