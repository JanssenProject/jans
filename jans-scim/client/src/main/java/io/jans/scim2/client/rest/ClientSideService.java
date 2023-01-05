/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest;

import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.bulk.BulkRequest;

import static io.jans.scim.model.scim2.Constants.*;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * A conglomerate interface that exhibits a rich amount of methods to manipulate User, Group, and Fido u2f and Fido 2
 * Device resources via the SCIM API. It also has support to call service metadata endpoints (see section 4 of RFC 7644).
 *
 * <p>The <i>ClientSide*</i> super interfaces add methods to actual interfaces used in server side implementation (those
 * in package {@link io.jans.scim.ws.rs.scim2 io.jans.scim.ws.rs.scim2}) enabling a more straightforward
 * interaction with the service by supplying Json payloads directly. This brings developers an alternative to the
 * objectual approach.</p>
 */
/*
 * Created by jgomer on 2017-09-04.
 */
public interface ClientSideService extends ClientSideUserService, ClientSideGroupService, ClientSideFidoDeviceService, ClientSideFido2DeviceService {

    /**
     * Performs a GET to the <code>/ServiceProviderConfig</code> endpoint that returns a JSON structure that describes
     * the SCIM specification features available on the target service implementation. See sections 5 and 8.5 of RFC 7643.
     * @return An object abstracting the response obtained from the server to this request.
     * A successful response for this request should contain a status code of 200 and a ServiceProviderConfig object
     * in the entity body
     */
    @Path("/v2/ServiceProviderConfig")
    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @FreelyAccessible
    Response getServiceProviderConfig();

    /**
     * Performs a GET to the <code>/ResourceTypes</code> endpoint that allows to discover the types of resources
     * available on the target service provider. See sections 6 and 8.6 of RFC 7643.
     * @return An object abstracting the response obtained from the server to this request.
     * A successful response for this request should contain a status code of 200 and a ListResponse in the entity body
     * (holding a collection of ResourceType objects)
     */
    @Path("/v2/ResourceTypes")
    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @FreelyAccessible
    Response getResourceTypes();

    /**
     * Performs a GET to the <code>/Schemas</code> endpoint that allows to retrieve information about resource schemas
     * supported by the service provider. See sections 7 and 8.7 of RFC 7643.
     * @return An object abstracting the response obtained from the server to this request.
     * A successful response for this request should contain a status code of 200 and a ListResponse in the entity body
     * (holding a collection of SchemaResource objects)
     */
    @Path("/v2/Schemas")
    @GET
    @Produces(MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT)
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @FreelyAccessible
    Response getSchemas();

    /**
     * Executes a system-wide query using HTTP POST. The results obtained can be of different resource types.
     * See section 3.4.3 of RFC 7644.
     * @param searchRequest An object containing the parameters for the query to execute. These are the same parameters
     *                      passed via URL for searches, for example in io.jans.scim.ws.rs.scim2.IUserWebService#searchUsers
     * @return An object abstracting the response obtained from the server to this request.
     * A successful response for this request should contain a status code of 200 and a ListResponse in the entity body
     * (holding a collection of SCIM resource objects)
     */
    @Path("/v2/.search")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response searchResourcesPost(SearchRequest searchRequest);

    /**
     * Executes a system-wide query using HTTP POST. This is analog to {@link #searchResourcesPost(SearchRequest) searchResourcesPost(SearchRequest)}
     * using a Json String to supply the payload.
     * @param searchRequestJson A String with the payload for the operation. It represents a <code>io.jans.scim.model.scim2.SearchRequest</code> object
     * @return An object abstracting the response obtained from the server to this request.
     * A successful response for this request should contain a status code of 200 and a ListResponse in the entity body
     * (holding a collection of SCIM resource objects)
     */
    @Path("/v2/.search")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response searchResourcesPost(String searchRequestJson);

    /**
     * Sends a bulk request as per section 3.7 of RFC 7644. This operation enables clients to send a potentially large
     * collection of resource operations in a single request.
     * @param request The object describing the request. Depending on the use case, constructing an instance of
     *                io.jans.scim.model.scim2.bulk.BulkRequest might be cumbersome. A more agile approach is using a
     *                Json string by calling {@link #processBulkOperations(String) processBulkOperations(String)}
     * @return An object abstracting the response obtained from the server to this request.
     * A successful response for this request should contain a status code of 200 and a BulkResponse object in the entity
     * body (holding the results of every processed operation). The number of results is constrained by parameters such as
     * io.jans.scim.model.scim2.bulk.BulkRequest#failOnErrors.
     */
    @Path("/v2/Bulk")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response processBulkOperations(BulkRequest request);

    /**
     * The analog to {@link #processBulkOperations(BulkRequest) processBulkOperations(BulkRequest)} using a Json payload.
     * @param requestJson A String with the payload for the operation. It represents a BulkRequest
     * @return An object abstracting the response obtained from the server to this request.
     * A successful response for this request should contain a status code of 200 and a BulkResponse object in the entity
     * body (holding the results of every processed operation). The number of results is constrained by parameters such as
     * io.jans.scim.model.scim2.bulk.BulkRequest#failOnErrors
     */
    @Path("/v2/Bulk")
    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    Response processBulkOperations(String requestJson);

    /**
     * Obtains user entries that have been updated or added in the local Gluu database after a specified 
     * timestamp. This is NOT part of SCIM spec. See class io.jans.scim.ws.rs.scim2.ScimResourcesUpdatedWebService
     * See the doc <a href="https://www.gluu.org/docs/gluu-server/user-management/idm-sync/">page</a>.
     * @param isoDate Represents a timestamp in ISO format (eg. 2019-12-24T12:00:03-05:00) 
     * @param start Integer offset from which results are output
     * @param itemsPerPage Maximum number of results to retrieve
     * @return An json object representing the results of the query. See the doc page for more information
     */    
    @Path("/v2/UpdatedUsers")
    @GET
    @Produces(MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT)
    Response usersChangedAfter(@QueryParam("timeStamp") String isoDate,
                                      @QueryParam("start") int start,
                                      @QueryParam("pageSize") int itemsPerPage);
    
}
