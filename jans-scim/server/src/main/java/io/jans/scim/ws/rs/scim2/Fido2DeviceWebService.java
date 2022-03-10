package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_ATTRIBUTES;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_COUNT;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_EXCLUDED_ATTRS;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_FILTER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_BY;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_SORT_ORDER;
import static io.jans.scim.model.scim2.Constants.QUERY_PARAM_START_INDEX;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.Instant;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.management.InvalidAttributeValueException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.GluuFido2Device;
import io.jans.scim.model.scim2.*;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.Fido2DeviceService;
import io.jans.scim.service.antlr.scimFilter.ScimFilterParserService;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.scim.service.scim2.interceptor.RefAdjusted;
import io.jans.scim.ws.rs.scim2.IFido2DeviceWebService;
import io.jans.scim.ws.rs.scim2.PATCH;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

/**
 * Implementation of /Fido2Devices endpoint. Methods here are intercepted.
 * Filter io.jans.scim.service.filter.AuthorizationProcessingFilter secures invocations
 */
@Named("scim2Fido2DeviceEndpoint")
@Path("/v2/Fido2Devices")
public class Fido2DeviceWebService extends BaseScimWebService implements IFido2DeviceWebService {

    @Inject
    private Fido2DeviceService fidoDeviceService;

    @Inject
    private ScimFilterParserService scimFilterParserService;

    @Inject
    private PersistenceEntryManager entryManager;

    private String fido2ResourceType;

    private Response doSearchDevices(String userId, String filter, Integer startIndex, 
            Integer count, String sortBy, String sortOrder, String attrsList, String excludedAttrsList,
            String method) {
        
        Response response;
        try {            
            SearchRequest searchReq = new SearchRequest();
            response = prepareSearchRequest(searchReq.getSchemas(), filter, sortBy,
                    sortOrder, startIndex, count, attrsList, excludedAttrsList, searchReq);
            if (response != null) return response;

            response = externalConstraintsService.applySearchCheck(searchReq,
                    httpHeaders, uriInfo, method, fido2ResourceType);
            if (response != null) return response;
            
            response = validateExistenceOfUser(userId);
            if (response != null) return response;

            PagedResult<BaseScimResource> resources = searchDevices(userId, searchReq.getFilter(), 
                    translateSortByAttribute(Fido2DeviceResource.class, searchReq.getSortBy()), 
                    SortOrder.getByValue(searchReq.getSortOrder()), searchReq.getStartIndex(),
                    searchReq.getCount());

            String json = getListResponseSerialized(resources.getTotalEntriesCount(), 
                    searchReq.getStartIndex(), resources.getEntries(), searchReq.getAttributesStr(),
                    searchReq.getExcludedAttributesStr(), searchReq.getCount() == 0);
            response = Response.ok(json).location(new URI(endpointUrl)).build();
        } catch (SCIMException e) {
            log.error(e.getMessage(), e);
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER,
            	e.getMessage());
        } catch (Exception e) {
            log.error("Failure at searchF2Devices method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
            	"Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.write"})
    public Response createDevice() {
        log.debug("Executing web service method. createDevice");
        return getErrorResponse(Response.Status.NOT_IMPLEMENTED, "Not implemented; device registration only happens via the FIDO 2.0 API.");
    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.read"})
    @RefAdjusted
    public Response getF2DeviceById(@PathParam("id") String id,
                                  @QueryParam("userId") String userId,
                                  @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
                                  @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. getF2DeviceById");

            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(userId, id);
            if (device == null) return notFoundResponse(id, fido2ResourceType);
            
            response = externalConstraintsService.applyEntityCheck(device, null,
                    httpHeaders, uriInfo, HttpMethod.GET, fido2ResourceType);
            if (response != null) return response;
            
            Fido2DeviceResource fidoResource = new Fido2DeviceResource();
            transferAttributesToFido2Resource(device, fidoResource, endpointUrl,
                userPersistenceHelper.getUserInumFromDN(device.getDn()));

            String json = resourceSerializer.serialize(fidoResource, attrsList, excludedAttrsList);
            response = Response.ok(new URI(fidoResource.getMeta().getLocation())).entity(json).build();
        } catch (Exception e) {
            log.error("Failure at getF2DeviceById method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.write"})
    @RefAdjusted
    public Response updateF2Device(
            Fido2DeviceResource fidoDeviceResource,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. updateDevice");

            //remove externalId, no place to store it in LDAP
            fidoDeviceResource.setExternalId(null);

            if (fidoDeviceResource.getId() != null && !fidoDeviceResource.getId().equals(id))
                throw new SCIMException("Parameter id does not match id attribute of Device");

            String userId = fidoDeviceResource.getUserId();
            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(userId, id);
            if (device == null) return notFoundResponse(id, fido2ResourceType);

            response = externalConstraintsService.applyEntityCheck(device, fidoDeviceResource,
                    httpHeaders, uriInfo, HttpMethod.PUT, fido2ResourceType);
            if (response != null) return response;
            
            executeValidation(fidoDeviceResource, true);

            Fido2DeviceResource updatedResource = new Fido2DeviceResource();
            transferAttributesToFido2Resource(device, updatedResource, endpointUrl, userId);

            updatedResource.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            updatedResource = (Fido2DeviceResource) ScimResourceUtil.transferToResourceReplace(fidoDeviceResource,
                    updatedResource, extService.getResourceExtensions(updatedResource.getClass()));
            transferAttributesToDevice(updatedResource, device);

            fidoDeviceService.updateFido2Device(device);

            String json = resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response = Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();
        } catch (SCIMException e) {
            log.error("Validation check error: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        } catch (InvalidAttributeValueException e) {
            log.error(e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        } catch (Exception e) {
            log.error("Failure at updateDevice method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.write"})
    public Response deleteF2Device(@PathParam("id") String id) {

        Response response;
        try {
            log.debug("Executing web service method. deleteDevice");

            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(null, id);
            if (device == null) return notFoundResponse(id, fido2ResourceType);

            response = externalConstraintsService.applyEntityCheck(device, null,
                    httpHeaders, uriInfo, HttpMethod.DELETE, fido2ResourceType);
            if (response != null) return response;

            fidoDeviceService.removeFido2Device(device);
            response = Response.noContent().build();
        } catch (Exception e) {
            log.error("Failure at deleteDevice method", e);
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, 
                    "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.read"})
    @RefAdjusted
    public Response searchF2Devices(
            @QueryParam("userId") String userId,
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        log.debug("Executing web service method. searchDevices");
        return doSearchDevices(userId, filter, startIndex, count, sortBy, sortOrder,
                attrsList, excludedAttrsList, HttpMethod.GET);

    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.read"})
    @RefAdjusted
    public Response searchF2DevicesPost(SearchRequest searchRequest, @QueryParam("userId") String userId) {

        log.debug("Executing web service method. searchDevicesPost");
        Response response = doSearchDevices(userId, searchRequest.getFilter(), searchRequest.getStartIndex(), 
                searchRequest.getCount(), searchRequest.getSortBy(), searchRequest.getSortOrder(), 
                searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(),
                HttpMethod.POST);

        URI uri = null;
        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        } catch (URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
        return Response.fromResponse(response).location(uri).build();

    }

    private void transferAttributesToFido2Resource(GluuFido2Device fidoDevice, Fido2DeviceResource res, String url, String userId) {

        res.setId(fidoDevice.getId());

        Meta meta=new Meta();
        meta.setResourceType(ScimResourceUtil.getType(res.getClass()));
        meta.setCreated(DateUtil.millisToISOString(fidoDevice.getCreationDate().getTime()));
        meta.setLastModified(DateUtil.millisToISOString(fidoDevice.getRegistrationData().getUpdatedDate().getTime()));
        meta.setLocation(url + "/" + fidoDevice.getId());

        res.setMeta(meta);
        res.setUserId(userId);
        res.setCreationDate(meta.getCreated());
        res.setCounter(fidoDevice.getRegistrationData().getCounter());

        res.setStatus(fidoDevice.getRegistrationStatus());
        res.setDisplayName(fidoDevice.getDisplayName());

    }

    private void transferAttributesToDevice(Fido2DeviceResource res, GluuFido2Device device){

        device.setId(res.getId());

        device.getRegistrationData().setCounter(res.getCounter());
        device.setRegistrationStatus(res.getStatus());
        device.setDisplayName(res.getDisplayName());
        
        Instant instant = Instant.parse(res.getMeta().getLastModified());
        device.getRegistrationData().setUpdatedDate(new Date(instant.toEpochMilli()));

    }

    private PagedResult<BaseScimResource> searchDevices(String userId, String filter, String sortBy, SortOrder sortOrder, int startIndex,
                                                        int count) throws Exception {

        Filter ldapFilter=scimFilterParserService.createFilter(filter, Filter.createPresenceFilter("jansId"), Fido2DeviceResource.class);
        log.info("Executing search for fido devices using: ldapfilter '{}', sortBy '{}', sortOrder '{}', startIndex '{}', count '{}', userId '{}'",
                ldapFilter.toString(), sortBy, sortOrder.getValue(), startIndex, count, userId);

        //workaround for https://github.com/GluuFederation/scim/issues/1: 
        //Currently, searching with SUB scope in Couchbase requires some help (beyond use of baseDN) 
        if (StringUtils.isNotEmpty(userId)) {
        	ldapFilter=Filter.createANDFilter(ldapFilter, Filter.createEqualityFilter("personInum", userId));
        }

        PagedResult<GluuFido2Device> list;
        try {
            list = entryManager.findPagedEntries(fidoDeviceService.getDnForFido2Device(null, userId),
                    GluuFido2Device.class, ldapFilter, null, sortBy, sortOrder, startIndex - 1, count, getMaxCount());
        } catch (Exception e) {
            log.info("Returning an empty listViewReponse");
            log.error(e.getMessage(), e);
            list = new PagedResult<>();
            list.setEntries(new ArrayList<>());
        }
        List<BaseScimResource> resources=new ArrayList<>();

        for (GluuFido2Device device : list.getEntries()){
            Fido2DeviceResource scimDev=new Fido2DeviceResource();
            transferAttributesToFido2Resource(device, scimDev, endpointUrl, userPersistenceHelper.getUserInumFromDN(device.getDn()));
            resources.add(scimDev);
        }
        log.info ("Found {} matching entries - returning {}", list.getTotalEntriesCount(), list.getEntries().size());

        PagedResult<BaseScimResource> result = new PagedResult<>();
        result.setEntries(resources);
        result.setTotalEntriesCount(list.getTotalEntriesCount());

        return result;

    }

    @Path("{id}")
    @PATCH
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.write"})
    @RefAdjusted
    public Response patchF2Device(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        log.debug("Executing web service method. patchDevice");
        return getErrorResponse(Response.Status.NOT_IMPLEMENTED, "Patch operation not supported for FIDO devices");
    }

    @PostConstruct
    public void setup(){
        init(Fido2DeviceWebService.class);
        fido2ResourceType = ScimResourceUtil.getType(Fido2DeviceResource.class);
    }

}
