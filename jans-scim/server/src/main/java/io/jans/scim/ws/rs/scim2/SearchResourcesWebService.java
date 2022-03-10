package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;
import static jakarta.ws.rs.core.Response.Status.OK;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.SearchRequest;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.model.scim2.util.ScimResourceUtil;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.scim.service.scim2.interceptor.RefAdjusted;
import io.jans.scim.service.scim2.serialization.ListResponseJsonSerializer;
import io.jans.util.Pair;

/**
 * Implementation of the /.search endpoint for the root URL of the service
 */
@Named
@Path("/v2/.search")
public class SearchResourcesWebService extends BaseScimWebService {

    @Inject
    private UserWebService userWS;

    @Inject
    private GroupWebService groupWS;

    @Inject
    private FidoDeviceWebService fidoWS;

    @Inject
    private Fido2DeviceWebService fido2WS;

    private ObjectMapper mapper=null;

    private int NUM_RESOURCE_TYPES;

    private Class resourceClasses[];

    @POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/all-resources.search"})
    @RefAdjusted
    public Response search(SearchRequest searchRequest) {

        SearchRequest searchReq = new SearchRequest();
        Response response = prepareSearchRequest(searchRequest.getSchemas(), searchRequest.getFilter(), searchRequest.getSortBy(),
                searchRequest.getSortOrder(), searchRequest.getStartIndex(), searchRequest.getCount(),
                searchRequest.getAttributesStr(), searchRequest.getExcludedAttributesStr(), searchReq);

        if (response == null) {
            try {
                List<JsonNode> resources = new ArrayList<>();
                Pair<Integer, Integer> totals = computeResults(searchReq, resources);

                ListResponseJsonSerializer custSerializer = new ListResponseJsonSerializer(resourceSerializer, searchReq.getAttributesStr(),
                        searchReq.getExcludedAttributesStr(), searchReq.getCount() == 0);
                if (resources.size() > 0)
                    custSerializer.setJsonResources(resources);

                ObjectMapper objmapper = new ObjectMapper();
                SimpleModule module = new SimpleModule("ListResponseModule", Version.unknownVersion());
                module.addSerializer(ListResponse.class, custSerializer);
                objmapper.registerModule(module);

                //Provide to constructor original start index, and totals calculated in computeResults call
                ListResponse listResponse = new ListResponse(searchReq.getStartIndex(), totals.getFirst(), totals.getSecond());
                String json = objmapper.writeValueAsString(listResponse);
                response = Response.ok(json).location(new URI(endpointUrl)).build();
            }
            catch (Exception e){
                log.error("Failure at search method", e);
                response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
            }
        }
        return response;

    }

    /**
     * Here we reuse every single POST search found in other web services, but handle serialization differently to a more
     * manual approach for performance reasons. In the end, this saves us 3 deserializations and 3 serializations of
     * multiple results packs.
     * Result set as a whole will not be sorted by sortBy param but every group of resources (by resource type) will be
     * sorted as such
     * @param searchRequest
     * @param resources
     * @return
     */
    private Pair<Integer, Integer> computeResults(SearchRequest searchRequest, List<JsonNode> resources) throws Exception{

        int i;
        int totalInPage=0, totalResults=0, skip=0;
        boolean resultsAvailable=false;

        Integer startIndex_=searchRequest.getStartIndex();
        JsonNode tree=null;

        //THIS ALGORITHM IS CONVOLUTED, IF YOU CHANGE IT ENSURE TEST CASES STILL PASS...

        //Move forward to skip the searches that might have no results and find the first one starting at index = searchRequest.getStartIndex()
        for (i=0; i< NUM_RESOURCE_TYPES && !resultsAvailable; i++) {
            tree=getListResponseTree(i, searchRequest);

            if (tree!=null) {
                totalResults += tree.get("totalResults").asInt();

                if (totalResults>0){

                    if (totalResults>=startIndex_) {
                        resultsAvailable = tree.get("itemsPerPage") != null;    //when null, it means searchRequest.getCount() was zero or empty page

                        if (searchRequest.getStartIndex()==1)
                            skip=startIndex_ - (totalResults - tree.get("totalResults").asInt()) - 1;
                    }
                    searchRequest.setStartIndex(1);     //Adjust startindex of subsequent searches to 1
                }
            }
        }

        if (resultsAvailable){

            //Accumulate till we have searchRequest.getCount() results or exhaust data

            Iterator<JsonNode> iterator = tree.get("Resources").elements();
            while (iterator.hasNext() && totalInPage < searchRequest.getCount()){
                if (skip==0) {
                    totalInPage++;
                    resources.add(iterator.next());
                }
                else{
                    skip--;
                    iterator.next();
                }
            }

            while (i< NUM_RESOURCE_TYPES && totalInPage < searchRequest.getCount()){

                resultsAvailable=false;
                tree = getListResponseTree(i, searchRequest);
                if (tree!=null) {
                    totalResults += tree.get("totalResults").asInt();

                    if (tree.get("totalResults").asInt() > 0)
                        resultsAvailable = tree.get("itemsPerPage")!=null;
                }

                if (resultsAvailable) {
                    for (iterator = tree.get("Resources").elements();
                         iterator.hasNext() && totalInPage < searchRequest.getCount();
                         totalInPage++)
                        resources.add(iterator.next());
                }

                i++;
            }

            //Continue the remainder of searches to just compute final value for totalResults
            while (i< NUM_RESOURCE_TYPES){
                tree = getListResponseTree(i, searchRequest);
                if (tree!=null)
                    totalResults += tree.get("totalResults").asInt();
                i++;
            }
        }

        //Revert startIndex to original
        searchRequest.setStartIndex(startIndex_);
        return new Pair<>(totalInPage, totalResults);

    }

    /**
     * Returns a JsonNode with the response obtained from sending a POST to a search method given the SearchRequest passed
     * @param index Determines the concrete search method to be executed: (0 - user; 1 - group; 2 - fido device)
     * @param searchRequest
     * @return
     */
    private JsonNode getListResponseTree(int index, SearchRequest searchRequest){

        try {
            log.debug("getListResponseTree. Resource type is: {}", ScimResourceUtil.getType(resourceClasses[index]));

            Response r = null;
            switch (index) {
                case 0:
                    r = userWS.searchUsersPost(searchRequest);
                    break;
                case 1:
                    r = groupWS.searchGroupsPost(searchRequest);
                    break;
                case 2:
                    r = fidoWS.searchDevicesPost(searchRequest, null);
                    break;
                case 3:
                    r = fido2WS.searchF2DevicesPost(searchRequest, null);
                    break;
            }

            if (r.getStatus()!=OK.getStatusCode())
                throw new Exception("Intermediate POST search returned " + r.getStatus());

            //readEntity does not work here since data is not backed by an input stream, so we just get the raw entity
            String jsonStr = r.getEntity().toString();
            return mapper.readTree(jsonStr);
        }
        catch (Exception e){
            log.error("Error in getListResponseTree {}", e.getMessage());
            log.error(e.getMessage(), e);
            return null;
        }

    }

    @PostConstruct
    public void setup(){
        //Do not use getClass() here... a typical weld issue...
        endpointUrl=appConfiguration.getBaseEndpoint() + SearchResourcesWebService.class.getAnnotation(Path.class).value();
        mapper=new ObjectMapper();

        //Do not alter the order of appearance (see getListResponseTree)
        resourceClasses=new Class[]{UserResource.class, GroupResource.class, FidoDeviceResource.class, Fido2DeviceResource.class};
        NUM_RESOURCE_TYPES =resourceClasses.length;
    }

}
