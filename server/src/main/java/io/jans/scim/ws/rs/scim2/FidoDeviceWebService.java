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
import java.util.List;

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

import org.apache.commons.lang.StringUtils;

import com.wordnik.swagger.annotations.ApiOperation;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.scim.model.exception.SCIMException;
import io.jans.scim.model.fido.GluuCustomFidoDevice;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ErrorScimType;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.Meta;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.util.DateUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.FidoDeviceService;
import io.jans.scim.service.antlr.scimFilter.ScimFilterParserService;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.scim.service.scim2.interceptor.RefAdjusted;

/**
 * Implementation of /FidoDevices endpoint. Methods here are intercepted and/or decorated.
 * Class org.gluu.oxtrust.service.scim2.interceptor.FidoDeviceWebServiceDecorator is used to apply pre-validations on data.
 * Filter org.gluu.oxtrust.ws.rs.scim2.AuthorizationProcessingFilter secures invocations
 *
 * @author Val Pecaoco
 * Updated by jgomer on 2017-10-09.
 */
@Named("scim2FidoDeviceEndpoint")
@Path("/v2/FidoDevices")
public class FidoDeviceWebService extends BaseScimWebService implements IFidoDeviceWebService {

    @Inject
	private FidoDeviceService fidoDeviceService;

    @Inject
    private ScimFilterParserService scimFilterParserService;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    private boolean ldapBackend;

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido.write"})
    @ApiOperation(value = "Create device", response = FidoDeviceResource.class)
    public Response createDevice() {
        log.debug("Executing web service method. createDevice");
        return getErrorResponse(Response.Status.NOT_IMPLEMENTED, "Not implemented; device registration only happens via the FIDO API.");
    }

    @Path("{id}")
    @GET
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido.read"})
    @RefAdjusted
    @ApiOperation(value = "Find device by id", notes = "Returns a device by id as path param", response = FidoDeviceResource.class)
    public Response getDeviceById(@PathParam("id") String id,
                           @QueryParam("userId") String userId,
                           @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
                           @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try{
            log.debug("Executing web service method. getDeviceById");
            FidoDeviceResource fidoResource=new FidoDeviceResource();

            GluuCustomFidoDevice device=fidoDeviceService.getGluuCustomFidoDeviceById(userId, id);
            //device cannot be null (see FidoDeviceWebServiceDecorator)

            transferAttributesToFidoResource(device, fidoResource, endpointUrl, getUserInumFromDN(device.getDn()));

            String json=resourceSerializer.serialize(fidoResource, attrsList, excludedAttrsList);
            response=Response.ok(new URI(fidoResource.getMeta().getLocation())).entity(json).build();
        }
        catch (SCIMException e){
            log.error(e.getMessage());
            response=getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        catch (Exception e){
            log.error("Failure at getDeviceById method", e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return response;

    }

    @Path("{id}")
    @PUT
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/fido.write"})
    @RefAdjusted
    @ApiOperation(value = "Update device", response = FidoDeviceResource.class)
    public Response updateDevice(
            FidoDeviceResource fidoDeviceResource,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        Response response;
        try {
            log.debug("Executing web service method. updateDevice");

            String userId=fidoDeviceResource.getUserId();
            GluuCustomFidoDevice device = fidoDeviceService.getGluuCustomFidoDeviceById(userId, id);
            if (device == null)
                throw new SCIMException("Resource " + id + " not found");

            FidoDeviceResource updatedResource=new FidoDeviceResource();
            transferAttributesToFidoResource(device, updatedResource, endpointUrl, userId);

            updatedResource.getMeta().setLastModified(DateUtil.millisToISOString(System.currentTimeMillis()));

            updatedResource=(FidoDeviceResource) ScimResourceUtil.transferToResourceReplace(fidoDeviceResource, updatedResource, extService.getResourceExtensions(updatedResource.getClass()));
            transferAttributesToDevice(updatedResource, device);

            fidoDeviceService.updateGluuCustomFidoDevice(device);

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
    @ProtectedApi(scopes = {"https://jans.io/scim/fido.write"})
    @ApiOperation(value = "Delete device")
    public Response deleteDevice(@PathParam("id") String id) {

        Response response;
        try {
            log.debug("Executing web service method. deleteDevice");

            //No need to check id being non-null. fidoDeviceService will give null if null is provided
            GluuCustomFidoDevice device = fidoDeviceService.getGluuCustomFidoDeviceById(null, id);
            if (device != null) {
                fidoDeviceService.removeGluuCustomFidoDevice(device);
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
    @ProtectedApi(scopes = {"https://jans.io/scim/fido.read"})
    @RefAdjusted
    @ApiOperation(value = "Search devices", notes = "Returns a list of devices", response = ListResponse.class)
    public Response searchDevices(
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
            sortBy=translateSortByAttribute(FidoDeviceResource.class, sortBy);
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
    @ProtectedApi(scopes = {"https://jans.io/scim/fido.read"})
    @RefAdjusted
    @ApiOperation(value = "Search devices POST /.search", notes = "Returns a list of fido devices", response = ListResponse.class)
    public Response searchDevicesPost(SearchRequest searchRequest, @QueryParam("userId") String userId) {

        log.debug("Executing web service method. searchDevicesPost");

        URI uri=null;
        Response response = searchDevices(userId, searchRequest.getFilter(), searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getSortBy(), searchRequest.getSortOrder(), searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr());

        try {
            uri = new URI(endpointUrl + "/" + SEARCH_SUFFIX);
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
        }
        return Response.fromResponse(response).location(uri).build();

    }

    private void transferAttributesToFidoResource(GluuCustomFidoDevice fidoDevice, FidoDeviceResource res, String url, String userId) {

        res.setId(fidoDevice.getId());

        Meta meta=new Meta();
        meta.setResourceType(ScimResourceUtil.getType(res.getClass()));

        String strDate = fidoDevice.getCreationDate();
        meta.setCreated(ldapBackend ? DateUtil.generalizedToISOStringDate(strDate) : (strDate + "Z"));
        meta.setLastModified(fidoDevice.getMetaLastModified());
        meta.setLocation(fidoDevice.getMetaLocation());
        if (meta.getLocation()==null)
            meta.setLocation(url + "/" + fidoDevice.getId());

        res.setMeta(meta);

        //Set values in order of appearance in FidoDeviceResource class
        res.setUserId(userId);
        res.setCreationDate(meta.getCreated());
        res.setApplication(fidoDevice.getApplication());
        res.setCounter(fidoDevice.getCounter());

        res.setDeviceData(fidoDevice.getDeviceData());
        res.setDeviceHashCode(fidoDevice.getDeviceHashCode());
        res.setDeviceKeyHandle(fidoDevice.getDeviceKeyHandle());
        res.setDeviceRegistrationConf(fidoDevice.getDeviceRegistrationConf());

        strDate = fidoDevice.getLastAccessTime();
        if (strDate != null) {
            res.setLastAccessTime(ldapBackend ? DateUtil.generalizedToISOStringDate(strDate) : (strDate + "Z"));
        }
        res.setStatus(fidoDevice.getStatus());
        res.setDisplayName(fidoDevice.getDisplayName());
        res.setDescription(fidoDevice.getDescription());
        res.setNickname(fidoDevice.getNickname());

    }

    /**
     * In practice, transference of values will not necessarily modify all original values in LDAP...
     * @param res
     * @param device
     */
    private void transferAttributesToDevice(FidoDeviceResource res, GluuCustomFidoDevice device){

        //Set values trying to follow the order found in GluuCustomFidoDevice class
        device.setId(res.getId());
        String strDate = res.getMeta().getCreated();
        device.setCreationDate(ldapBackend ? DateUtil.ISOToGeneralizedStringDate(strDate) : DateUtil.gluuCouchbaseISODate(strDate));
        device.setApplication(res.getApplication());
        device.setCounter(res.getCounter());

        device.setDeviceData(res.getDeviceData());
        device.setDeviceHashCode(res.getDeviceHashCode());
        device.setDeviceKeyHandle(res.getDeviceKeyHandle());
        device.setDeviceRegistrationConf(res.getDeviceRegistrationConf());

        strDate = res.getLastAccessTime();
        device.setLastAccessTime(ldapBackend ? DateUtil.ISOToGeneralizedStringDate(strDate) : DateUtil.gluuCouchbaseISODate(strDate));
        device.setStatus(res.getStatus());
        device.setDisplayName(res.getDisplayName());
        device.setDescription(res.getDescription());
        device.setNickname(res.getNickname());

        device.setMetaLastModified(res.getMeta().getLastModified());
        device.setMetaLocation(res.getMeta().getLocation());
        device.setMetaVersion(res.getMeta().getVersion());

    }

    private PagedResult<BaseScimResource> searchDevices(String userId, String filter, String sortBy, SortOrder sortOrder, int startIndex,
                                                    int count, String url) throws Exception {

        Filter ldapFilter=scimFilterParserService.createFilter(filter, Filter.createPresenceFilter("jansId"), FidoDeviceResource.class);
        log.info("Executing search for fido devices using: ldapfilter '{}', sortBy '{}', sortOrder '{}', startIndex '{}', count '{}', userId '{}'",
                ldapFilter.toString(), sortBy, sortOrder.getValue(), startIndex, count, userId);

        //workaround for https://github.com/GluuFederation/scim/issues/1: 
        //Currently, searching with SUB scope in Couchbase requires some help (beyond use of baseDN) 
        if (StringUtils.isNotEmpty(userId)) {
        	ldapFilter=Filter.createANDFilter(ldapFilter, Filter.createEqualityFilter("personInum", userId));
        }
        
        PagedResult<GluuCustomFidoDevice> list;
        try {
            list = ldapEntryManager.findPagedEntries(fidoDeviceService.getDnForFidoDevice(userId, null),
                    GluuCustomFidoDevice.class, ldapFilter, null, sortBy, sortOrder, startIndex - 1, count, getMaxCount());
        } catch (Exception e) {
            log.info("Returning an empty listViewReponse");
            log.error(e.getMessage(), e);
            list = new PagedResult<>();
            list.setEntries(new ArrayList<>());
        }
        List<BaseScimResource> resources=new ArrayList<>();

        for (GluuCustomFidoDevice device : list.getEntries()){
            FidoDeviceResource scimDev=new FidoDeviceResource();
            transferAttributesToFidoResource(device, scimDev, url, getUserInumFromDN(device.getDn()));
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
    @ProtectedApi
    @RefAdjusted
    @ApiOperation(value = "PATCH operation", notes = "https://tools.ietf.org/html/rfc7644#section-3.5.2", response = FidoDeviceResource.class)
    public Response patchDevice(
            PatchRequest request,
            @PathParam("id") String id,
            @QueryParam(QUERY_PARAM_ATTRIBUTES) String attrsList,
            @QueryParam(QUERY_PARAM_EXCLUDED_ATTRS) String excludedAttrsList){

        log.debug("Executing web service method. patchDevice");
        return getErrorResponse(Response.Status.NOT_IMPLEMENTED, "Patch operation not supported for FIDO devices");
    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here... a typical weld issue...
        endpointUrl=appConfiguration.getBaseEndpoint() + FidoDeviceWebService.class.getAnnotation(Path.class).value();
        ldapBackend = scimFilterParserService.isLdapBackend();
    }

}
