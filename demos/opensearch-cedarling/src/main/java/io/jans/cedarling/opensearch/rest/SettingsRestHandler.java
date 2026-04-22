package io.jans.cedarling.opensearch.rest;

import io.jans.cedarling.opensearch.*;

import java.io.IOException;
import java.util.*;
import java.net.URLEncoder; 

import org.json.*;
import org.opensearch.action.admin.cluster.settings.*;
import org.opensearch.common.action.*;
import org.opensearch.common.xcontent.json.*;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.rest.*;
import org.opensearch.rest.*;
import org.opensearch.transport.client.*;
import org.opensearch.transport.client.node.NodeClient;

import static io.jans.cedarling.opensearch.CedarlingPlugin.NAME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.opensearch.rest.RestRequest.Method.*;

public class SettingsRestHandler extends BaseRestHandler {
    
    private static final String PATH = "/_plugins/" + URLEncoder.encode(NAME, UTF_8) + "/settings";
    private static final long TIMEOUT = 1500;   //1.5 seconds
    
    @Override
    public String getName() {
        return "cedarling_settings_handler";
    }

    @Override
    public List routes() {
        return List.of(new RestHandler.Route(GET, PATH), new RestHandler.Route(PUT, PATH));
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        
        RestRequest.Method method = request.method();
        switch (method) {
            case RestRequest.Method.PUT:
                return handlePut(request);
            case RestRequest.Method.GET:
                return handleGet(request);
            default:  
                return ch -> ch.sendResponse(
                        new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, "Method not allowed:" + method.toString()));
        }

    }
 
    private RestChannelConsumer handlePut(RestRequest request) {

        return channel -> {
            try {
                logger.info("Handling PUT request");
                boolean hasJsonHeader = Optional.ofNullable(request.getAllHeaderValues("Content-Type"))
                        .map(l -> l.contains("application/json")).orElse(false);
                BytesRestResponse response;
                
                if (hasJsonHeader) {
                    String payload = request.content().utf8ToString();
                    logger.debug("Payload size is {}", payload.length());
                    
                    JSONObject job = new JSONObject(payload);   //ensure it is really JSON content
                    Long now = System.currentTimeMillis();
                    
                    ClusterUpdateSettingsRequest cusr = new ClusterUpdateSettingsRequest();
                    cusr.persistentSettings(Map.of(
                        CedarlingPlugin.SETTINGS_KEY, (Object) payload, CedarlingPlugin.LAST_UPDATED_KEY, (Object) now
                    ));
                    
                    ClusterAdminClient caca = CedarlingPlugin.getClusterAdminClient();
                    logger.debug("Sending update settings request to cluster...");
                    
                    ClusterUpdateSettingsResponse updateResponse = caca.updateSettings(cusr).actionGet(TIMEOUT);
                    boolean acknowledged = updateResponse.isAcknowledged();
                    logger.info("Response acknowledged: {}", acknowledged);

                    response = new BytesRestResponse(
                        acknowledged ? RestStatus.OK : RestStatus.INTERNAL_SERVER_ERROR,
                        "application/json",
                        BytesReference.bytes(JsonXContent.contentBuilder().map(Map.of("acknowledged", acknowledged)))
                    );

                } else {
                    response = new BytesRestResponse(RestStatus.NOT_ACCEPTABLE, "Unexpected Content-Type header");
                }
                
                logger.debug("Sending PUT response...");
                channel.sendResponse(response);
            } catch (Exception e) {
                channel.sendResponse(new BytesRestResponse(channel, e));
            }
        };
        
    }
    
 
    private RestChannelConsumer handleGet(RestRequest request) {

        return channel -> {
            try {
                logger.info("Handling GET request");
                Map<String, Object> map = Optional.ofNullable(SettingsService.getInstance().getSettings())
                        .map(PluginSettings::asMap).orElse(Collections.emptyMap());
                
                if (map.isEmpty()) {
                    logger.warn("There was a problem retrieving Cedarling plugin settings, or they have not been defined yet");
                }
                    
                BytesRestResponse response = new BytesRestResponse(
                    RestStatus.OK,
                    "application/json",
                    BytesReference.bytes(JsonXContent.contentBuilder().map(map))
                );

                logger.debug("Sending GET response...");
                channel.sendResponse(response);
            } catch (Exception e) {
                channel.sendResponse(new BytesRestResponse(channel, e));
                //channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "Error processing audit request: " + e.getMessage()));
            }
        };

    }

}
