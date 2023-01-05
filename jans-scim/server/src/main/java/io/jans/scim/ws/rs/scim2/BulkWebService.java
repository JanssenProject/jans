package io.jans.scim.ws.rs.scim2;

import static io.jans.scim.model.scim2.Constants.MEDIA_TYPE_SCIM_JSON;
import static io.jans.scim.model.scim2.Constants.UTF8_CHARSET_FRAGMENT;
import static io.jans.scim.ws.rs.scim2.BulkWebService.Verb.DELETE;
import static io.jans.scim.ws.rs.scim2.BulkWebService.Verb.PATCH;
import static io.jans.scim.ws.rs.scim2.BulkWebService.Verb.POST;
import static io.jans.scim.ws.rs.scim2.BulkWebService.Verb.PUT;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.REQUEST_ENTITY_TOO_LARGE;
import static jakarta.ws.rs.core.Response.Status.Family.CLIENT_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static jakarta.ws.rs.core.Response.Status.Family.familyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.StringUtils;
import io.jans.scim.model.scim2.ErrorScimType;
import io.jans.scim.model.scim2.bulk.BulkOperation;
import io.jans.scim.model.scim2.bulk.BulkRequest;
import io.jans.scim.model.scim2.bulk.BulkResponse;
import io.jans.scim.model.scim2.fido.FidoDeviceResource;
import io.jans.scim.model.scim2.fido.Fido2DeviceResource;
import io.jans.scim.model.scim2.group.GroupResource;
import io.jans.scim.model.scim2.patch.PatchRequest;
import io.jans.scim.model.scim2.user.UserResource;
import io.jans.scim.service.filter.ProtectedApi;
import io.jans.util.Pair;

/**
 * SCIM Bulk Endpoint Implementation
 */
@Named("scim2BulkEndpoint")
@Path("/v2/Bulk")
public class BulkWebService extends BaseScimWebService {

    enum Verb {POST, PUT, PATCH, DELETE}    //HTTP methods involved in bulk requests

    private final Pattern bulkIdPattern = Pattern.compile("bulkId:(\\w+)");

    private List<Verb> availableMethods;
    private ObjectMapper mapper = new ObjectMapper();

    private String usersEndpoint;
    private String groupsEndpoint;
    private String fidodevicesEndpoint;
    private String fido2devicesEndpoint;
    private String commonWsEndpointPrefix;

    @Inject
    private UserWebService userWS;

    @Inject
    private GroupWebService groupWS;

    @Inject
    private FidoDeviceWebService fidoDeviceWS;
    
    @Inject
    private Fido2DeviceWebService fido2DeviceWS;

    @jakarta.ws.rs.POST
    @Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
    @Produces({MEDIA_TYPE_SCIM_JSON + UTF8_CHARSET_FRAGMENT, MediaType.APPLICATION_JSON + UTF8_CHARSET_FRAGMENT})
    @HeaderParam("Accept") @DefaultValue(MEDIA_TYPE_SCIM_JSON)
    @ProtectedApi(scopes = {"https://jans.io/scim/bulk"})
    public Response processBulkOperations(BulkRequest request) {

        Response response=prepareRequest(request, getValueFromHeaders(httpHeaders, "Content-Length"));
        if (response==null) {
            log.debug("Executing web service method. processBulkOperations");

            int i, errors=0;
            List<BulkOperation> operations=request.getOperations();
            List<BulkOperation> responseOperations=new ArrayList<>();
            Map<String, String> processedBulkIds=new HashMap<>();

            for (i=0;i<operations.size() && errors<request.getFailOnErrors();i++){

                BulkOperation operation=operations.get(i);
                BulkOperation operationResponse=new BulkOperation();
                Response subResponse;

                String method=operation.getMethod();
                String bulkId=operation.getBulkId();
                try {
                    String path=operation.getPath();
                    BaseScimWebService service=getWSForPath(path);
                    String fragment=getFragment(path, service, processedBulkIds);
                    Verb verb = Verb.valueOf(method);

                    String data=operation.getDataStr();
                    if (!verb.equals(DELETE))
                        data = replaceBulkIds(data, processedBulkIds);

                    Pair<Response, String> pair=execute(verb, service, data, fragment);
                    String idCreated=pair.getSecond();
                    subResponse=pair.getFirst();
                    int status=subResponse.getStatus();

                    if (familyOf(status).equals(SUCCESSFUL)) {
                        if (!verb.equals(DELETE)) {
                            if (verb.equals(POST)) {  //Update bulkIds
                                processedBulkIds.put(bulkId, idCreated);
                                fragment=idCreated;
                            }
                            String loc=service.getEndpointUrl() + "/" + fragment;
                            operationResponse.setLocation(loc);
                        }
                    }
                    else {
                        operationResponse.setResponse(subResponse.getEntity());
                        errors+= familyOf(status).equals(CLIENT_ERROR) || familyOf(status).equals(SERVER_ERROR) ? 1 : 0;
                    }

                    subResponse.close();
                    operationResponse.setStatus(Integer.toString(status));
                }
                catch (Exception e) {
                    log.error(e.getMessage(), e);
                    subResponse=getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, e.getMessage());

                    operationResponse.setStatus(Integer.toString(BAD_REQUEST.getStatusCode()));
                    operationResponse.setResponse(subResponse.getEntity());
                    errors++;
                }

                operationResponse.setBulkId(bulkId);
                operationResponse.setMethod(method);

                responseOperations.add(operationResponse);

                log.debug("Operation {} processed with status {}. Method {}, Accumulated errors {}", i+1, operationResponse.getStatus(), method, errors);
            }

            try {
                BulkResponse bulkResponse=new BulkResponse();
                bulkResponse.setOperations(responseOperations);

                String json = mapper.writeValueAsString(bulkResponse);
                response=Response.ok(json).build();
            }
            catch (Exception e){
                log.error(e.getMessage(), e);
                response=getErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage());
            }
        }
        return response;

    }

    private Response prepareRequest(BulkRequest request, String contentLength) {

        Response response=null;

        if (request.getFailOnErrors()==null)
            request.setFailOnErrors(appConfiguration.getBulkMaxOperations());

        List<BulkOperation> operations=request.getOperations();

        if (operations==null || operations.isEmpty())
            response=getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_VALUE, "No operations supplied");
        else {

            long contentLen;
            try{
//log.debug("CONT LEN {}", contentLength);
                contentLen=Long.valueOf(contentLength);
            }
            catch (Exception e){
                contentLen=appConfiguration.getBulkMaxPayloadSize();
            }

            boolean payloadExceeded=contentLen > appConfiguration.getBulkMaxPayloadSize();
            boolean operationsExceeded=operations.size() > appConfiguration.getBulkMaxOperations();
            StringBuilder sb=new StringBuilder();

            if (payloadExceeded)
                sb.append("The size of the bulk operation exceeds the maxPayloadSize (").
                        append(appConfiguration.getBulkMaxPayloadSize()).append(" bytes). ");
            if (operationsExceeded)
                sb.append("The number of operations exceed the maxOperations value (").
                        append(appConfiguration.getBulkMaxOperations()).append("). ");

            if (sb.length()>0)
                response=getErrorResponse(REQUEST_ENTITY_TOO_LARGE, sb.toString());
        }
        if (response==null) {
            try {

                for (BulkOperation operation : operations) {

                    if (operation==null)
                        throw new Exception("An operation passed was found to be null");

                    String path = operation.getPath();
                    if (StringUtils.isEmpty(path))
                        throw new Exception("path parameter is required");

                    path=adjustPath(path);
                    operation.setPath(path);

                    String method = operation.getMethod();
                    if (StringUtils.isNotEmpty(method)) {
                        method = method.toUpperCase();
                        operation.setMethod(method);
                    }

                    Verb verb = Verb.valueOf(method);
                    if (!availableMethods.contains(verb))
                        throw new Exception("method not recognized: " + method);

                    //Check if path passed is consistent with respect to method:
                    List<String> availableEndpoints=Arrays.asList(usersEndpoint, groupsEndpoint, fidodevicesEndpoint, fido2devicesEndpoint);
                    boolean consistent = false;
                    for (String endpoint : availableEndpoints) {
                        if (verb.equals(POST))
                            consistent = path.equals(endpoint);
                        else   //Checks if there is something after the additional slash
                            consistent = path.startsWith(endpoint + "/") && (path.length() > endpoint.length() + 1);

                        if (consistent)
                            break;
                    }
                    if (!consistent)
                        throw new Exception("path parameter is not consistent with method " + method);

                    //Check if bulkId must be present
                    String bulkId = operation.getBulkId();
                    if (StringUtils.isEmpty(bulkId) && verb.equals(POST))
                        throw new Exception("bulkId parameter is required for method " + method);

                    //Check if data must be present
                    String data=operation.getDataStr();
                    List<Verb> dataMethods=Arrays.asList(POST, PUT, PATCH);
                    if (dataMethods.contains(verb) && StringUtils.isEmpty(data))
                        throw new Exception("data parameter is required for method " + method);
                }
            }
            catch (Exception e) {
                response=getErrorResponse(BAD_REQUEST, ErrorScimType.INVALID_SYNTAX, e.getMessage());
            }
        }
        return response;

    }

    private BaseScimWebService getWSForPath(String path){
        if (path.startsWith(usersEndpoint))
            return userWS;
        else
        if (path.startsWith(groupsEndpoint))
            return groupWS;
        else
        if (path.startsWith(fidodevicesEndpoint))
            return fidoDeviceWS;
        else
        if (path.startsWith(fido2devicesEndpoint))
            return fido2DeviceWS;
        else
            return null;
    }

    private String adjustPath(String path){
        return path.startsWith(commonWsEndpointPrefix) ? path : commonWsEndpointPrefix + path;
    }

    private String getFragment(String path, BaseScimWebService service, Map<String, String> idsMap) throws Exception{
        int endpointLen=service.getEndpointUrl().length()+1;
        String frag=(path.length() > endpointLen) ? path.substring(endpointLen) : "";
        return replaceBulkIds(frag, idsMap);
    }

    private String replaceBulkIds(String str, Map<String, String> idsMap) throws Exception{

        Matcher m=bulkIdPattern.matcher(str);
        StringBuffer sb = new StringBuffer();

        while (m.find()){
            String id=m.group(1);
            //See if the id supplied is known
            String realId=idsMap.get(id);
            if (realId==null)
                throw new Exception("bulkId '" + id + "' not recognized");

            m.appendReplacement(sb, realId);
        }
        m.appendTail(sb);

        return sb.toString();

    }

    private Pair<Response, String> execute(Verb verb, BaseScimWebService ws, String data, String fragment) {

        Response response=null;
        String idCreated=null;

        try {
            if (ws==userWS)
                switch (verb){
                    case PUT:
                        UserResource user=mapper.readValue(data, UserResource.class);
                        response=userWS.updateUser(user, fragment, "id", null);
                        break;
                    case DELETE:
                        response=userWS.deleteUser(fragment);
                        break;
                    case PATCH:
                        PatchRequest pr=mapper.readValue(data, PatchRequest.class);
                        response=userWS.patchUser(pr, fragment, "id", null);
                        break;
                    case POST:
                        user=mapper.readValue(data, UserResource.class);
                        response=userWS.createUser(user, "id", null);
                        if (CREATED.getStatusCode()==response.getStatus()) {
                            user = mapper.readValue(response.getEntity().toString(), UserResource.class);
                            idCreated = user.getId();
                        }
                        break;
                }

            else
            if (ws==groupWS)
                switch (verb){
                    case PUT:
                        GroupResource group=mapper.readValue(data, GroupResource.class);
                        response=groupWS.updateGroup(group, fragment, "id", null);
                        break;
                    case DELETE:
                        response=groupWS.deleteGroup(fragment);
                        break;
                    case PATCH:
                        PatchRequest pr=mapper.readValue(data, PatchRequest.class);
                        response=groupWS.patchGroup(pr, fragment, "id", null);
                        break;
                    case POST:
                        group=mapper.readValue(data, GroupResource.class);
                        response=groupWS.createGroup(group, "id", null);
                        if (CREATED.getStatusCode()==response.getStatus()) {
                            group = mapper.readValue(response.getEntity().toString(), GroupResource.class);
                            idCreated = group.getId();
                        }
                        break;
                }

            else
            if (ws==fidoDeviceWS)
                switch (verb){
                    case PUT:
                        FidoDeviceResource dev=mapper.readValue(data, FidoDeviceResource.class);
                        response=fidoDeviceWS.updateDevice(dev, fragment, "id", null);
                        break;
                    case DELETE:
                        response=fidoDeviceWS.deleteDevice(fragment);
                        break;
                    case PATCH:
                        PatchRequest pr=mapper.readValue(data, PatchRequest.class);
                        response=fidoDeviceWS.patchDevice(pr, fragment, "id", null);
                        break;
                    case POST:
                        response=fidoDeviceWS.createDevice();
                        break;
                }

            else
            if (ws==fido2DeviceWS)
                switch (verb){
                    case PUT:
                        Fido2DeviceResource dev=mapper.readValue(data, Fido2DeviceResource.class);
                        response=fido2DeviceWS.updateF2Device(dev, fragment, "id", null);
                        break;
                    case DELETE:
                        response=fido2DeviceWS.deleteF2Device(fragment);
                        break;
                    case PATCH:
                        PatchRequest pr=mapper.readValue(data, PatchRequest.class);
                        response=fido2DeviceWS.patchF2Device(pr, fragment, "id", null);
                        break;
                    case POST:
                        response=fido2DeviceWS.createDevice();
                        break;
                }
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
            response=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
        return new Pair<>(response, idCreated);

    }

    @PostConstruct
    public void setup() {
        //Do not use getClass() here... a typical weld issue...
        endpointUrl=appConfiguration.getBaseEndpoint() + BulkWebService.class.getAnnotation(Path.class).value();
        availableMethods= Arrays.asList(Verb.values());

        usersEndpoint=userWS.getEndpointUrl();
        groupsEndpoint=groupWS.getEndpointUrl();
        fidodevicesEndpoint=fidoDeviceWS.getEndpointUrl();
        fido2devicesEndpoint=fido2DeviceWS.getEndpointUrl();
        commonWsEndpointPrefix=usersEndpoint.substring(0, usersEndpoint.lastIndexOf("/"));
    }

}
