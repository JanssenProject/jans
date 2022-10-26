/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.PATCH_REQUEST_SCHEMA_ID;
import static io.jans.scim.model.scim2.Constants.SEARCH_REQUEST_SCHEMA_ID;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.Path;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.jans.scim.model.conf.AppConfiguration;
import io.jans.orm.model.SortOrder;
import io.jans.scim.model.GluuCustomPerson;
import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ErrorResponse;
import io.jans.scim.model.scim2.ErrorScimType;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.Meta;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.patch.PatchOperation;
import io.jans.scim.model.scim2.patch.PatchOperationType;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ResourceValidator;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.service.PersonService;
import io.jans.scim.service.antlr.scimFilter.util.FilterUtil;
import io.jans.scim.service.scim2.ExtensionService;
import io.jans.scim.service.scim2.ExternalConstraintsService;
import io.jans.scim.service.scim2.UserPersistenceHelper;
import io.jans.scim.service.scim2.serialization.ListResponseJsonSerializer;
import io.jans.scim.service.scim2.serialization.ScimResourceSerializer;

/**
 * Base methods for SCIM web services
 *
 * @author Yuriy Movchan Date: 08/23/2013
 * Re-engineered by jgomer on 2017-09-14.
 */
@Dependent
public class BaseScimWebService {

    @Inject
    Logger log;

    @Inject
    AppConfiguration appConfiguration;

    @Inject
    ScimResourceSerializer resourceSerializer;

    @Inject
    ExtensionService extService;

    @Inject
    PersonService personService;

    @Inject
    UserPersistenceHelper userPersistenceHelper;

    @Inject
    ExternalConstraintsService externalConstraintsService;

    @Context
    HttpHeaders httpHeaders;

    @Context
    UriInfo uriInfo;

    public static final String SEARCH_SUFFIX = ".search";
    
    private static final String CN_ENV_VAR = "CN_VERSION";

    String endpointUrl;

    public String getEndpointUrl() {
        return endpointUrl;
    }
    
    public Response notFoundResponse(String id, String resourceType) {
        
        log.info("{} with inum {} not found", resourceType, id);
        return getErrorResponse(Response.Status.NOT_FOUND, 
                String.format("%s with id %s not found", resourceType, id));
        
    }

    public static Response getErrorResponse(Response.Status status, String detail) {
        return getErrorResponse(status.getStatusCode(), null, detail);
    }

    public static Response getErrorResponse(Response.Status status, ErrorScimType scimType, String detail) {
        return getErrorResponse(status.getStatusCode(), scimType, detail);
    }

    public static Response getErrorResponse(int statusCode, ErrorScimType scimType, String detail) {

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(String.valueOf(statusCode));
        errorResponse.setScimType(scimType);
        errorResponse.setDetail(detail);

        return Response.status(statusCode).entity(errorResponse).build();
    }

    public Response validateExistenceOfUser(String id) {

        Response response = null;
        if (StringUtils.isNotEmpty(id)) {
            GluuCustomPerson person = personService.getPersonByInum(id);

            if (person == null) {
                log.info("Person with inum {} not found", id);
                response = getErrorResponse(Response.Status.NOT_FOUND, "User with id " + id + " not found");
            }
        }
        return response;

    }

    int getMaxCount(){
        return appConfiguration.getMaxCount();
    }

    String getValueFromHeaders(HttpHeaders headers, String name){
        List<String> values=headers.getRequestHeaders().get(name);
        return (values==null || values.size()==0) ? null : values.get(0);
    }
    
    protected void init(Class<? extends BaseScimWebService> cls) {

    	if (endpointUrl == null) {
			String base = appConfiguration.getBaseEndpoint(); 
			base = System.getenv(CN_ENV_VAR) == null ? base : base.replaceFirst("/identity", "/scim");
			endpointUrl = base + cls.getAnnotation(Path.class).value();
    	}

    }
    
    protected void assignMetaInformation(BaseScimResource resource){

        //Generate some meta information (this replaces the info client passed in the request)
        String val = DateUtil.millisToISOString(System.currentTimeMillis());

        Meta meta=new Meta();
        meta.setResourceType(ScimResourceUtil.getType(resource.getClass()));
        meta.setCreated(val);
        meta.setLastModified(val);
        //For version attritute: Service provider support for this attribute is optional and subject to the service provider's support for versioning
        //For location attribute: this will be set after current user creation in LDAP
        resource.setMeta(meta);

    }

    protected void executeValidation(BaseScimResource resource) throws SCIMException {
        executeValidation(resource, false);
    }

    protected void executeValidation(BaseScimResource resource, boolean laxRequiredness) throws SCIMException {

        ResourceValidator rv=new ResourceValidator(resource, extService.getResourceExtensions(resource.getClass()));
        if (!laxRequiredness){
            rv.validateSchemasAttribute();
        }
        rv.validateRequiredAttributes(laxRequiredness);
        rv.validateValidableAttributes();
        //By section 7 of RFC 7643, we are not forced to constrain attribute values when they have a list of canonical values associated
        //rv.validateCanonicalizedAttributes();
        rv.validateExtendedAttributes();

    }

    //Transform scim attribute to LDAP attribute
    String translateSortByAttribute(Class<? extends BaseScimResource> cls, String sortBy) {

        String type=ScimResourceUtil.getType(cls);
        if (StringUtils.isEmpty(sortBy) || type==null)
            sortBy=null;
        else {
            if (extService.extensionOfAttribute(cls, sortBy)==null) {   //It's not a custom attribute...

                sortBy=ScimResourceUtil.stripDefaultSchema(cls, sortBy);
                Field f=IntrospectUtil.findFieldFromPath(cls, sortBy);

                if (f==null){   //Not recognized!
                    log.warn("SortBy parameter value '{}' was not recognized as a SCIM attribute for resource {} - sortBy will be ignored.", sortBy, type);
                    sortBy=null;
                    //return getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_PATH, "sortBy parameter value not recognized");
                }
                else {
                    sortBy = FilterUtil.getLdapAttributeOfResourceAttribute(sortBy, cls).getFirst();
                    if (sortBy==null)
                        log.warn("There is no LDAP attribute mapping to sortBy attribute provided - sortBy will be ignored.");
                }
            }
            else
                sortBy = sortBy.substring(sortBy.lastIndexOf(":")+1);
        }
        return sortBy;

    }

    protected Response prepareSearchRequest(List<String> schemas, String filter, 
    	String sortBy, String sortOrder, Integer startIndex, Integer count,
        String attrsList, String excludedAttrsList, SearchRequest request) {

        Response response = null;

        if (schemas != null && schemas.size() == 1 && schemas.get(0).equals(SEARCH_REQUEST_SCHEMA_ID)) {
            count = count == null ? getMaxCount() : count;
            //Per spec, a negative value SHALL be interpreted as "0" for count
            if (count < 0) {
                count = 0;
            }

            if (count <= getMaxCount()) {
                //SCIM searches are 1 indexed
                startIndex = (startIndex == null || startIndex < 1) ? 1 : startIndex;

                if (StringUtils.isEmpty(sortOrder) || !sortOrder.equals(SortOrder.DESCENDING.getValue())) {
                    sortOrder = SortOrder.ASCENDING.getValue();
                }

                request.setSchemas(schemas);
                request.setAttributes(attrsList);
                request.setExcludedAttributes(excludedAttrsList);
                request.setFilter(filter);
                request.setSortBy(sortBy);
                request.setSortOrder(sortOrder);
                request.setStartIndex(startIndex);
                request.setCount(count);
            } else {
                response = getErrorResponse(BAD_REQUEST, ErrorScimType.TOO_MANY, "Maximum number of results per page is " + getMaxCount());
            }
        } else {
            response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Wrong schema(s) supplied in Search Request");
        }
        return response;

    }

    String getListResponseSerialized(int total, int startIndex, List<BaseScimResource> resources, String attrsList,
                                     String excludedAttrsList, boolean ignoreResults) throws IOException{

        ListResponse listResponse = new ListResponse(startIndex, resources.size(), total);
        listResponse.setResources(resources);

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("ListResponseModule", Version.unknownVersion());
        module.addSerializer(ListResponse.class, new ListResponseJsonSerializer(resourceSerializer, attrsList, excludedAttrsList, ignoreResults));
        mapper.registerModule(module);

        return mapper.writeValueAsString(listResponse);

    }

    protected Response inspectPatchRequest(PatchRequest patch, Class<? extends BaseScimResource> cls){

        Response response=null;
        List<String> schemas=patch.getSchemas();

        if (schemas!=null && schemas.size()==1 && schemas.get(0).equals(PATCH_REQUEST_SCHEMA_ID)) {
            List<PatchOperation> ops = patch.getOperations();

            if (ops != null) {
                //Adjust paths if they came prefixed

                String defSchema=ScimResourceUtil.getDefaultSchemaUrn(cls);
                List<String> urns=extService.getUrnsOfExtensions(cls);
                urns.add(defSchema);

                for (PatchOperation op : ops){
                    if (op.getPath()!=null)
                        op.setPath(ScimResourceUtil.adjustNotationInPath(op.getPath(), defSchema, urns));
                }

                for (PatchOperation op : ops) {

                    if (op.getType() == null)
                        response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Operation '" + op.getOperation() + "' not recognized");
                    else {
                        String path = op.getPath();

                        if (StringUtils.isEmpty(path) && op.getType().equals(PatchOperationType.REMOVE))
                            response = getErrorResponse(BAD_REQUEST, ErrorScimType.NO_TARGET, "Path attribute is required for remove operation");
                        else
                        if (op.getValue() == null && !op.getType().equals(PatchOperationType.REMOVE))
                            response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Value attribute is required for operations other than remove");
                    }
                    if (response != null)
                        break;
                }
            }
            else
                response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Patch request MUST contain the attribute 'Operations'");
        }
        else
            response = getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, "Wrong schema(s) supplied in Search Request");

        log.info("inspectPatchRequest. Preprocessing of patch request {}", response==null ? "passed" : "failed");
        return response;

    }

}
