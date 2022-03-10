/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import javax.lang.model.type.NullType;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import io.jans.scim.model.scim2.AttributeDefinition;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.Meta;
import io.jans.scim.model.scim2.annotations.Attribute;
import io.jans.scim.model.scim2.annotations.Schema;
import io.jans.scim.model.scim2.extensions.Extension;
import io.jans.scim.model.scim2.extensions.ExtensionField;
import io.jans.scim.model.scim2.provider.config.ServiceProviderConfig;
import io.jans.scim.model.scim2.provider.resourcetypes.ResourceType;
import io.jans.scim.model.scim2.provider.schema.SchemaAttribute;
import io.jans.scim.model.scim2.provider.schema.SchemaResource;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.scim2.interceptor.RejectFilterParam;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Web service for the /Schemas endpoint.
 */
@Named("scim2SchemaEndpoint")
@Path("/v2/Schemas")
public class SchemaWebService extends BaseScimWebService {

    private Map<String, Class<? extends BaseScimResource>> resourceSchemas;

    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response serve(){

        Response response;
        try {
            int total = resourceSchemas.size();
            ListResponse listResponse = new ListResponse(1, total, total);

            for (String urn : resourceSchemas.keySet()){
                listResponse.addResource(getSchemaInstance(resourceSchemas.get(urn), urn));
            }
            String json=resourceSerializer.getListResponseMapper().writeValueAsString(listResponse);
            response=Response.ok(json).location(new URI(endpointUrl)).build();
        }
        catch (Exception e){
            log.error("Failure at serve method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;
    }

    @GET
    @Path("{schemaUrn}")
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @RejectFilterParam
    public Response getSchemaById(@PathParam("schemaUrn") String urn){

        Response response;
        try {
            Class<? extends BaseScimResource> cls = resourceSchemas.get(urn);

            if (cls==null){
                log.info("Schema urn {} not recognized", urn);
                response=Response.status(Response.Status.NOT_FOUND).build();
            }
            else {
                String json=resourceSerializer.serialize(getSchemaInstance(cls, urn));
                URI location = new URI(endpointUrl + "/" + urn);
                response = Response.ok(json).location(location).build();
            }
        }
        catch (Exception e){
            log.error("Failure at getSchemaById method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;
    }

    @PostConstruct
    public void setup() {
        //Do not use getClass() here... a typical weld issue...
        endpointUrl = appConfiguration.getBaseEndpoint() + SchemaWebService.class.getAnnotation(Path.class).value();

        List<Class<? extends BaseScimResource>> excludedResources=Arrays.asList(SchemaResource.class, ResourceType.class, ServiceProviderConfig.class);
        resourceSchemas=new HashMap<>();

        //Fill map with urn vs. resource
        for (Class<? extends BaseScimResource> cls : IntrospectUtil.allAttrs.keySet()){
            if (!excludedResources.contains(cls)) {
                resourceSchemas.put(ScimResourceUtil.getDefaultSchemaUrn(cls), cls);

                for (Extension extension : extService.getResourceExtensions(cls))
                    resourceSchemas.put(extension.getUrn(), cls);
            }
        }

    }

    private SchemaResource getSchemaInstance(Class<? extends BaseScimResource> clazz) throws Exception{

        SchemaResource resource;
        Class<? extends BaseScimResource> schemaCls=SchemaResource.class;
        Schema annotation=ScimResourceUtil.getSchemaAnnotation(clazz);
        if (!clazz.equals(schemaCls) && annotation!=null){

            Meta meta=new Meta();
            meta.setResourceType(ScimResourceUtil.getType(schemaCls));
            meta.setLocation(endpointUrl + "/" + annotation.id());

            resource=new SchemaResource();
            resource.setId(annotation.id());
            resource.setName(annotation.name());
            resource.setDescription(annotation.description());
            resource.setMeta(meta);

            List<SchemaAttribute> attribs=new ArrayList<>();
            //paths are, happily alphabetically sorted :)
            for (String path : IntrospectUtil.allAttrs.get(clazz)){
                SchemaAttribute schAttr=new SchemaAttribute();
                Field f=IntrospectUtil.findFieldFromPath(clazz, path);

                Attribute attrAnnot=f.getAnnotation(Attribute.class);
                if (attrAnnot!=null) {
                    JsonProperty jsonAnnot=f.getAnnotation(JsonProperty.class);

                    schAttr.setName(jsonAnnot==null ? f.getName() : jsonAnnot.value());
                    schAttr.setType(attrAnnot.type().getName());
                    schAttr.setMultiValued(!attrAnnot.multiValueClass().equals(NullType.class) || IntrospectUtil.isCollection(f.getType()));
                    schAttr.setDescription(attrAnnot.description());
                    schAttr.setRequired(attrAnnot.isRequired());

                    schAttr.setCanonicalValues(attrAnnot.canonicalValues().length==0 ? null : Arrays.asList(attrAnnot.canonicalValues()));
                    schAttr.setCaseExact(attrAnnot.isCaseExact());
                    schAttr.setMutability(attrAnnot.mutability().getName());
                    schAttr.setReturned(attrAnnot.returned().getName());
                    schAttr.setUniqueness(attrAnnot.uniqueness().getName());
                    schAttr.setReferenceTypes(attrAnnot.referenceTypes().length==0 ? null : Arrays.asList(attrAnnot.referenceTypes()));

                    if (attrAnnot.type().equals(AttributeDefinition.Type.COMPLEX))
                        schAttr.setSubAttributes(new ArrayList<>());

                    List<SchemaAttribute> list=attribs;     //root list
                    String parts[]=path.split("\\.");

                    for (int i=0;i<parts.length-1;i++) {    //skip last part (real attribute name)
                        int j = list.indexOf(new SchemaAttribute(parts[i]));
                        list = list.get(j).getSubAttributes();
                    }

                    list.add(schAttr);
                }
            }
            resource.setAttributes(attribs);
        }
        else
            resource=null;

        return resource;
    }

    private SchemaResource getSchemaInstance(Class<? extends BaseScimResource> clazz, String urn) throws Exception{

        if (ScimResourceUtil.getDefaultSchemaUrn(clazz).equals(urn))
            return getSchemaInstance(clazz);    //Process core attributes
        else{   //process extension attributes
            SchemaResource resource=null;
            Class<? extends BaseScimResource> schemaCls=SchemaResource.class;

            //Find the appropriate extension
            List<Extension> extensions=extService.getResourceExtensions(clazz);
            for (Extension extension : extensions) {
                if (extension.getUrn().equals(urn)) {

                    Meta meta = new Meta();
                    meta.setResourceType(ScimResourceUtil.getType(schemaCls));
                    meta.setLocation(endpointUrl + "/" + urn);

                    resource = new SchemaResource();
                    resource.setId(urn);
                    resource.setName(extension.getName());
                    resource.setDescription(extension.getDescription());
                    resource.setMeta(meta);

                    List<SchemaAttribute> attribs = new ArrayList<>();

                    for (ExtensionField field : extension.getFields().values()) {
                        SchemaAttribute schAttr = new SchemaAttribute();

                        schAttr.setName(field.getName());
                        schAttr.setMultiValued(field.isMultiValued());
                        schAttr.setDescription(field.getDescription());
                        schAttr.setRequired(false);

                        schAttr.setCanonicalValues(null);
                        schAttr.setCaseExact(false);
                        schAttr.setMutability(AttributeDefinition.Mutability.READ_WRITE.getName());
                        schAttr.setReturned(AttributeDefinition.Returned.DEFAULT.getName());
                        schAttr.setUniqueness(AttributeDefinition.Uniqueness.NONE.getName());
                        schAttr.setReferenceTypes(null);

                        AttributeDefinition.Type type = field.getAttributeDefinitionType();
                        schAttr.setType(type==null ? null : type.getName());
                        schAttr.setSubAttributes(null);

                        attribs.add(schAttr);
                    }

                    resource.setAttributes(attribs);
                    break;
                }
            }
            return resource;
        }
    }

}
