/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.util.Collections;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import io.jans.scim.model.scim2.Meta;
import io.jans.scim.model.scim2.provider.config.AuthenticationScheme;
import io.jans.scim.model.scim2.provider.config.ServiceProviderConfig;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.scim2.interceptor.RejectFilterParam;

@Named("serviceProviderConfig")
@Path("/v2/ServiceProviderConfig")
public class ServiceProviderConfigWS extends BaseScimWebService {

    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response serve(){

        try {
            ServiceProviderConfig serviceProviderConfig = new ServiceProviderConfig();
            serviceProviderConfig.getFilter().setMaxResults(appConfiguration.getMaxCount());
            serviceProviderConfig.getBulk().setMaxOperations(appConfiguration.getBulkMaxOperations());
            serviceProviderConfig.getBulk().setMaxPayloadSize(appConfiguration.getBulkMaxPayloadSize());

            Meta meta = new Meta();
            meta.setLocation(endpointUrl);
            meta.setResourceType(ScimResourceUtil.getType(serviceProviderConfig.getClass()));
            serviceProviderConfig.setMeta(meta);

            serviceProviderConfig.setAuthenticationSchemes(Collections.singletonList(
                    AuthenticationScheme.createOAuth2(true)));

            return Response.ok(resourceSerializer.serialize(serviceProviderConfig)).build();
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }

    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here... a typical weld issue...
        endpointUrl=appConfiguration.getBaseEndpoint() + ServiceProviderConfigWS.class.getAnnotation(Path.class).value();
    }

}
