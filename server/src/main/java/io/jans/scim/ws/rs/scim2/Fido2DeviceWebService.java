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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.InvalidAttributeValueException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.ApiOperation;

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
 * Implementation of /Fido2Devices endpoint. Methods here are intercepted and/or decorated.
 * Class org.gluu.oxtrust.service.scim2.interceptor.Fido2DeviceWebServiceDecorator is used to apply pre-validations on data.
 * Filter org.gluu.oxtrust.ws.rs.scim2.AuthorizationProcessingFilter secures invocations
 *
 * @author jgomer
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

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.write"})
    @ApiOperation(value = "Create device", response = Fido2DeviceResource.class)
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
    @ApiOperation(value = "Find device by id", notes = "Returns a device by id as path param", response = Fido2DeviceResource.class)
    public Response getF2DeviceById(@PathParam("id") String id,
                                  @QueryParam("userId") String userId,
                                  @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
                                  @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try{
            log.debug("Executing web service method. getF2DeviceById");
            Fido2DeviceResource fidoResource=new Fido2DeviceResource();

            GluuFido2Device device=fidoDeviceService.getFido2DeviceById(userId, id);
            //device cannot be null (see Fido2DeviceWebServiceDecorator)

            transferAttributesToFido2Resource(device, fidoResource, endpointUrl, getUserInumFromDN(device.getDn()));

            String json=resourceSerializer.serialize(fidoResource, attrsList, excludedAttrsList);
            response=Response.ok(new URI(fidoResource.getMeta().getLocation())).entity(json).build();
        }
        catch (SCIMException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at getF2DeviceById method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
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
    @ApiOperation(value = "Update device", response = Fido2DeviceResource.class)
    public Response updateF2Device(
            Fido2DeviceResource fidoDeviceResource,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try {
            log.debug("Executing web service method. updateDevice");

            String userId=fidoDeviceResource.getUserId();
            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(userId, id);
            if (device == null)
                throw new SCIMException("Resource " + id + " not found");

            Fido2DeviceResource updatedResource=new Fido2DeviceResource();
            transferAttributesToFido2Resource(device, updatedResource, endpointUrl, userId);

            updatedResource.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            updatedResource= (Fido2DeviceResource) ScimResourceUtil.transferToResourceReplace(fidoDeviceResource,
                    updatedResource, extService.getResourceExtensions(updatedResource.getClass()));
            transferAttributesToDevice(updatedResource, device);

            fidoDeviceService.updateFido2Device(device);

            String json=resourceSerializer.serialize(updatedResource, attrsList, excludedAttrsList);
            response=Response.ok(new URI(updatedResource.getMeta().getLocation())).entity(json).build();
        }
        catch (SCIMException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        catch (InvalidAttributeValueException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.MUTABILITY, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at updateDevice method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @DELETE
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.write"})
    @ApiOperation(value = "Delete device")
    public Response deleteF2Device(@PathParam("id") String id) {

        Response response;
        try {
            log.debug("Executing web service method. deleteDevice");

            //No need to check id being non-null. fidoDeviceService will give null if null is provided
            GluuFido2Device device = fidoDeviceService.getFido2DeviceById(null, id);
            if (device != null) {
                fidoDeviceService.removeFido2Device(device);
                response = Response.noContent().build();
            }
            else
                response = getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, "Resource " + id + " not found");
        }
        catch (Exception e){
            log.error("Failure at deleteDevice method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.read"})
    @RefAdjusted
    @ApiOperation(value = "Search devices", notes = "Returns a list of devices", response = ListResponse.class)
    public Response searchF2Devices(
            @QueryParam("userId") String userId,
            @QueryParam(QUERY_PARAM_FILTER) String filter,
            @QueryParam(QUERY_PARAM_START_INDEX) Integer startIndex,
            @QueryParam(QUERY_PARAM_COUNT) Integer count,
            @QueryParam(QUERY_PARAM_SORT_BY) String sortBy,
            @QueryParam(QUERY_PARAM_SORT_ORDER) String sortOrder,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList) {

        Response response;
        try {
            log.debug("Executing web service method. searchDevices");
            sortBy=translateSortByAttribute(Fido2DeviceResource.class, sortBy);
            PagedResult<BaseScimResource> resources = searchDevices(userId, filter, sortBy, SortOrder.getByValue(sortOrder), startIndex, count, endpointUrl);

            String json = getListResponseSerialized(resources.getTotalEntriesCount(), startIndex, resources.getEntries(), attrsList, excludedAttrsList, count==0);
            response=Response.ok(json).location(new URI(endpointUrl)).build();
        }
        catch (SCIMException e){
            log.error(e.getMessage(), e);
            response=getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_FILTER, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at searchDevices method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path(SEARCH_SUFFIX)
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido2.read"})
    @RefAdjusted
    @ApiOperation(value = "Search devices POST /.search", notes = "Returns a list of fido devices", response = ListResponse.class)
    public Response searchF2DevicesPost(SearchRequest searchRequest, @QueryParam("userId") String userId) {

        log.debug("Executing web service method. searchDevicesPost");

        URI uri=null;
        Response response = searchF2Devices(userId, searchRequest.getFilter(), searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getSortBy(), searchRequest.getSortOrder(), searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr());

        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        }
        catch (Exception e){
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
                                                        int count, String url) throws Exception {

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
            transferAttributesToFido2Resource(device, scimDev, url, getUserInumFromDN(device.getDn()));
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
    @ApiOperation(value = "PATCH operation", notes = "https://tools.ietf.org/html/rfc7644#section-3.5.2", response = Fido2DeviceResource.class)
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
        endpointUrl=appConfiguration.getBaseEndpoint() + Fido2DeviceWebService.class.getAnnotation(Path.class).value();
    }

}
