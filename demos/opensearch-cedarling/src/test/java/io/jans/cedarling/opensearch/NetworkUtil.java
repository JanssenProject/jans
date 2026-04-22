package io.jans.cedarling.opensearch;

import com.nimbusds.oauth2.sdk.http.*;
import com.nimbusds.common.contenttype.ContentType;

import java.util.*;
import java.net.URL;

import org.apache.logging.log4j.*;
import org.json.JSONObject;

public class NetworkUtil {
    
    private static final int RESPONSE_TRUNCATE_LEN = 180;
    private Logger logger = LogManager.getLogger(getClass());
    
    private int connectionTimeout;
    private int readTimeout;
    private String host;
    private String authzHeader;
    
    public NetworkUtil(String host, String authzHeader) {
        this(host, authzHeader, 4500, 30000);
    }
    
    public NetworkUtil(String host, String authzHeader, int connectionTimeout, int readTimeout) {
        this.host = host;
        this.authzHeader = authzHeader;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }
    
    public JSONObject sendDelete(String uri, int... expectedStatus) throws Exception {
        return send(uri, expectedStatus, HTTPRequest.Method.DELETE, null);
    }
    
    public JSONObject sendPost(String uri, int expectedStatus, String jsonPayload) throws Exception {
        return send(uri, new int[] { expectedStatus }, HTTPRequest.Method.POST, jsonPayload);
    }
    
    private JSONObject send(String uri, int[] expectedStatus, HTTPRequest.Method method, String jsonPayload)
        throws Exception {
        
        HTTPRequest request = new HTTPRequest(method, new URL(host + "/" + uri));
        request.setConnectTimeout(connectionTimeout);
        request.setReadTimeout(readTimeout);
        request.setHeader("Authorization", authzHeader);
        
        if (jsonPayload != null) {
            request.setBody(jsonPayload);
            request.setHeader("Content-Type", "application/json");
        }
        
        HTTPResponse response = request.send();
        response.ensureStatusCode(expectedStatus);
        response.ensureEntityContentType(ContentType.APPLICATION_JSON);
        
        //response.getHeaderValue("Content-Type");
        String body = response.getBody();
        String truncated = body;
        
        if (body.length() > RESPONSE_TRUNCATE_LEN) {            
            truncated = body.substring(0, RESPONSE_TRUNCATE_LEN) + " ..."; 
        }
        
        logger.info("HTTP response [{}] {}", response.getStatusCode(), truncated);
        return new JSONObject(body);
        
    }

}
