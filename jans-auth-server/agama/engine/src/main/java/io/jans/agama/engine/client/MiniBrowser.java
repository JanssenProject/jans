package io.jans.agama.engine.client;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import io.jans.util.Pair;

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.Status.Family;
import java.io.IOException;
import java.net.*;
import java.util.*;

import org.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nimbusds.oauth2.sdk.http.HTTPRequest.Method.*;
import static java.nio.charset.StandardCharsets.UTF_8;

class WebResponse {
    
    private int status;
    private String body;
    private String contentType;
    
    private WebResponse() { }
    
    static WebResponse from(HTTPResponse response) {
        
        WebResponse wr = new WebResponse();
        wr.status = response.getStatusCode();
        wr.contentType = response.getHeaderValue(HttpHeaders.CONTENT_TYPE);
        wr.body = response.getBody();
        return wr;

    }
    
    public int getStatus() {
        return status;
    }
    
    public String getBody() {
        return body;
    }
    
    public String getContentType() {
        return contentType;
    }
    
}

/**
 * A micro HTTP client capable of interacting with the Agama engine in order to run JSON-based
 * flows. It lessens the effort of exchanging messages with the engine and serves as a utility 
 * to make possible the fact of running Agama flows in native applications.<br>
 * The 'move' method implements the POST-REDIRECT-GET pattern of the engine: at every step, the
 * URL passed is supplied with the given JSON contents, and the HTTP redirect is followed. The
 * method returns one of the possible outcomes (see Outcome enum) plus some JSON data.<br>
 * This is an explanation of outcomes: CLIENT_ERROR (problems to connect to the URL or read the 
 * response), ENGINE_ERROR (the flow crashed, timed out, or an RFAC instruction was reached),
 * FLOW_FINISHED (a Finish instruction was executed), and FLOW_PAUSED (RRF instruction was hit).<br>
 * The JSON data returned by 'move' contains error data (CLIENT_ERROR or ENGINE_ERROR), the data
 * associated to the Finish instruction (FLOW_FINISHED), or the data supplied to the RRF instruction
 * (FLOW_PAUSED). Only in the last case the method may receive a subsequent invocation, where the 
 * JSON data to supply is supposed to emulate the output of the RRF execution, that is, the result
 * of having submitted a UI form in the app (desktop or mobile).<br>
 * Thus, it is the native app that takes charge of the UI rendering by receiving the same data
 * the equivalent Freemarker template would receive (in the web world), and the data of the form 
 * submission is built by the native app too. In this case the path to the UI template (in RRF)
 * has no effect, but it is anyways included in the output of 'move' for reference.
 */
public class MiniBrowser {
    
    public enum Outcome { CLIENT_ERROR, ENGINE_ERROR, FLOW_FINISHED, FLOW_PAUSED }
    
    public static final String FLOW_PAUSED_URL_KEY = "_url";
    
    private static final Logger logger = LoggerFactory.getLogger(MiniBrowser.class);
    
    private String rootUrl;
    private int connectionTimeout;
    private int readTimeout;
    private int maxErrorContentLength;
    
    public MiniBrowser(String rootUrl) {
        this(rootUrl, 3500, 3500, 4096);
    }
    
    public MiniBrowser(String rootUrl, 
                int connectionTimeout, int readTimeout, int maxErrorContentLength) {
    
        this.rootUrl = rootUrl;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.maxErrorContentLength = maxErrorContentLength;

    }
        
    public Pair<Outcome, JSONObject> move(String phantomSid, String relativeUrl, String jsonPayload) {
        
        try {
            return moveImpl(phantomSid, relativeUrl, jsonPayload);
        } catch (Exception e) {
            String error = e.getMessage();
            logger.error(error, e);
            
            JSONObject jobj = new JSONObject(Map.of("description", error));
            return new Pair<>(Outcome.CLIENT_ERROR, jobj);
        }
        
    }
        
    private Pair<Outcome, JSONObject> moveImpl(String phantomSid, String relativeUrl, String jsonPayload)
            throws Exception {

        String error = null;
        String url = normalize(relativeUrl);        
        logger.info("Moving forward from {}", url);

        HTTPResponse response = sendRequest(phantomSid, new URL(url), jsonPayload);
        WebResponse wr = WebResponse.from(response);
        int status = wr.getStatus();
        
        if (Family.familyOf(status).equals(Family.REDIRECTION)) {
            String location = response.getHeaderValue(HttpHeaders.LOCATION);

            if (location != null) {
                wr = null;
                logger.info("Redirecting to {}", location);

                response = sendRequest(phantomSid, new URL(normalize(location)), null);
                wr = WebResponse.from(response);
                
                if (MediaType.APPLICATION_JSON.equals(wr.getContentType()) && wr.getStatus() == 200) {
                    
                    logger.info("Returning JSON contents");
                    JSONObject jobj = new JSONObject(wr.getBody());

                    jobj.put(FLOW_PAUSED_URL_KEY, location);
                    return new Pair<>(Outcome.FLOW_PAUSED, jobj);
                }

                error = "Expecting OK JSON response for " + location;

            } else {
                error = "Target of redirection is missing";
            }
        } else if (MediaType.APPLICATION_JSON.equals(wr.getContentType()) && status == 200) {
            
            logger.info("Seems to have landed to the finish page");            
            JSONObject jobj = new JSONObject(wr.getBody());
            
            if (jobj.has("success")) return new Pair<>(Outcome.FLOW_FINISHED, jobj);

            error = "Unexpected response to " + url;
            
        } else {
            error = "Unexpected response to " + url;
        }

        logger.error(error);
        JSONObject jobj = new JSONObject(Map.of("description", error));
        
        String contentType = wr.getContentType();            
        jobj.put("status", wr.getStatus());
        jobj.put("contentType", Optional.ofNullable((Object) contentType).orElse(JSONObject.NULL));

        String body = wr.getBody();
        if (body == null) {
            jobj.put("body", JSONObject.NULL);
        } else if (MediaType.APPLICATION_JSON.equals(contentType)) {
            jobj.put("body", new JSONObject(body));
        } else {
            body = body.substring(0, Math.min(body.length(), maxErrorContentLength));
            jobj.put("body", body);
        }

        return new Pair<>(Outcome.ENGINE_ERROR, jobj);

    }
    
    private HTTPResponse sendRequest(String phantomSid, URL url, String jsonPayload) throws IOException {

        boolean noPayload = jsonPayload == null;
        HTTPRequest request = new HTTPRequest(noPayload ? GET : POST, url);
        request.setConnectTimeout(connectionTimeout);
        request.setReadTimeout(readTimeout);
        //Ideally, redirects should be followed, but cookies are lost between requests :(
        request.setFollowRedirects(false);
        //... and without following redirects, the content-type has to be passed at every request :(
        
        //the presence of this header signals the engine not to read the incoming data as application/x-www-form-urlencoded
        //and also to use the json version of engine's error pages
        request.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        //sending the session_id cookie helps maintain the state of the running flow between server/client
        request.setHeader​(HttpHeaders.COOKIE, String.format("session_id=%s;", phantomSid));
            
        if (!noPayload) {
            request.setBody(jsonPayload);
        }
        return request.send();
        
    }
    
    private String normalize(String relativeUrl) {        
        String url = relativeUrl.startsWith(rootUrl) ? "" : rootUrl;
        return url + relativeUrl;
    }
        
}
