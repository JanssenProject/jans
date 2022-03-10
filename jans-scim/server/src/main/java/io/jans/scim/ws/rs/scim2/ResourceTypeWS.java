/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.Meta;
import io.jans.scim.model.scim2.annotations.Schema;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.provider.resourcetypes.ResourceType;
import io.jans.scim.model.scim2.provider.resourcetypes.SchemaExtensionHolder;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.scim2.ExtensionService;
import io.jans.scim.service.scim2.interceptor.RejectFilterParam;

@Named("resourceTypesWs")
@Path("/v2/ResourceTypes")
public class ResourceTypeWS extends BaseScimWebService {

    //The following are not computed using the endpointUrl's of web services since they are required to be constant (used in @Path annotations)
    private static final String USER_SUFFIX="User";
    private static final String GROUP_SUFFIX="Group";
    private static final String FIDO_SUFFIX="FidoDevice";
    private static final String FIDO2_SUFFIX="Fido2Device";

    @Inject
    private UserWebService userService;

    @Inject
    private GroupWebService groupService;

    @Inject
    private FidoDeviceWebService fidoService;

    @Inject
    private Fido2DeviceWebService fido2Service;

    @Inject
    private ExtensionService extService;

    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response serve() {

        try {
            ListResponse listResponse = new ListResponse(1, 4, 4);
            listResponse.addResource(getUserResourceType());
            listResponse.addResource(getGroupResourceType());
            listResponse.addResource(getFidoDeviceResourceType());
            listResponse.addResource(getFido2DeviceResourceType());

            String json = resourceSerializer.getListResponseMapper().writeValueAsString(listResponse);
            return Response.ok(json).location(new URI(endpointUrl)).build();
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }

    }

    @Path(USER_SUFFIX)
    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response userResourceType() {

        try {
            URI uri=new URI(getResourceLocation(USER_SUFFIX));
            return Response.ok(resourceSerializer.serialize(getUserResourceType())).location(uri).build();
        }
        catch (Exception e){
            log.error("Failure at userResourceType method", e);
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }

    }

    @Path(GROUP_SUFFIX)
    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response groupResourceType() {

        try {
            URI uri=new URI(getResourceLocation(GROUP_SUFFIX));
            return Response.ok(resourceSerializer.serialize(getGroupResourceType())).location(uri).build();
        }
        catch (Exception e){
            log.error("Failure at groupResourceType method", e);
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }

    }

    @Path(FIDO_SUFFIX)
    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response fidoResourceType() {
        try {
            URI uri=new URI(getResourceLocation(FIDO_SUFFIX));
            return Response.ok(resourceSerializer.serialize(getFidoDeviceResourceType())).location(uri).build();
        }
        catch (Exception e){
            log.error("Failure at fidoResourceType method", e);
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }

    }

    @Path(FIDO2_SUFFIX)
    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response fido2ResourceType() {
        try {
            URI uri=new URI(getResourceLocation(FIDO2_SUFFIX));
            return Response.ok(resourceSerializer.serialize(getFido2DeviceResourceType())).location(uri).build();
        }
        catch (Exception e){
            log.error("Failure at fido2ResourceType method", e);
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }

    }

    @PostConstruct
    public void setup(){
        //weld makes you cry if using getClass() here
        endpointUrl=appConfiguration.getBaseEndpoint() + ResourceTypeWS.class.getAnnotation(Path.class).value();
    }

    private void fillResourceType(ResourceType rt, Schema schemaAnnot, String endpointUrl, String location, List<SchemaExtensionHolder> schemaExtensions){

        rt.setId(schemaAnnot.name());
        rt.setName(schemaAnnot.name());
        rt.setDescription(schemaAnnot.description());
        rt.setEndpoint(endpointUrl.substring(appConfiguration.getBaseEndpoint().length()));
        rt.setSchema(schemaAnnot.id());
        rt.setSchemaExtensions(schemaExtensions);

        Meta rtMeta = new Meta();
        rtMeta.setLocation(location);
        rtMeta.setResourceType("ResourceType");
        rt.setMeta(rtMeta);

    }

    private ResourceType getUserResourceType(){

        Class<? extends BaseScimResource> cls=UserResource.class;
        List<Extension> usrExtensions=extService.getResourceExtensions(cls);
        List<SchemaExtensionHolder> schemaExtensions=new ArrayList<>();

        for (Extension extension : usrExtensions){
            SchemaExtensionHolder userExtensionSchema = new SchemaExtensionHolder();
            userExtensionSchema.setSchema(extension.getUrn());
            userExtensionSchema.setRequired(false);

            schemaExtensions.add(userExtensionSchema);
        }

        ResourceType usrRT = new ResourceType();
        fillResourceType(usrRT, ScimResourceUtil.getSchemaAnnotation(cls), userService.getEndpointUrl(), getResourceLocation(USER_SUFFIX), schemaExtensions);
        return usrRT;

    }

    private ResourceType getGroupResourceType(){
        ResourceType grRT = new ResourceType();
        fillResourceType(grRT, ScimResourceUtil.getSchemaAnnotation(GroupResource.class), groupService.getEndpointUrl(), getResourceLocation(GROUP_SUFFIX), null);
        return grRT;
    }

    private ResourceType getFidoDeviceResourceType(){
        ResourceType fidoRT = new ResourceType();
        fillResourceType(fidoRT, ScimResourceUtil.getSchemaAnnotation(FidoDeviceResource.class), fidoService.getEndpointUrl(), getResourceLocation(FIDO_SUFFIX), null);
        return fidoRT;
    }

    private ResourceType getFido2DeviceResourceType(){
        ResourceType fido2RT = new ResourceType();
        fillResourceType(fido2RT, ScimResourceUtil.getSchemaAnnotation(Fido2DeviceResource.class), fido2Service.getEndpointUrl(), getResourceLocation(FIDO2_SUFFIX), null);
        return fido2RT;
    }

    private String getResourceLocation(String suffix){
        return endpointUrl + "/" + suffix;
    }

}
