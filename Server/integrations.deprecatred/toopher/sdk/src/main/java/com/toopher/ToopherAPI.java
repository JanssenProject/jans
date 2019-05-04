package com.toopher;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A Java binding for the Toopher API
 * 
 */
public class ToopherAPI {
    /**
     * The ToopherJava binding library version
     */
    public static final String VERSION = "1.0.0";

    /**
     * Create an API object with the supplied credentials
     * 
     * @param consumerKey
     *            The consumer key for a requester (obtained from the developer portal)
     * @param consumerSecret
     *            The consumer secret for a requester (obtained from the developer portal)
     */
    public ToopherAPI(String consumerKey, String consumerSecret) {
        httpClient = new DefaultHttpClient();
        HttpProtocolParams.setUserAgent(httpClient.getParams(),
                                        String.format("ToopherJava/%s", VERSION));

        consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
    }

    /**
     * Create a pairing
     * 
     * @param pairingPhrase
     *            The pairing phrase supplied by the user
     * @param userName
     *            A user-facing descriptive name for the user (displayed in requests)
     * @return A PairingStatus object
     * @throws RequestError
     *             Thrown when an exceptional condition is encountered
     */
    public PairingStatus pair(String pairingPhrase, String userName) throws RequestError {
        final String endpoint = "pairings/create";

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("pairing_phrase", pairingPhrase));
        params.add(new BasicNameValuePair("user_name", userName));

        try {
            JSONObject json = post(endpoint, params);
            return PairingStatus.fromJSON(json);
        } catch (Exception e) {
            throw new RequestError(e);
        }
    }

    /**
     * Retrieve the current status of a pairing request
     * 
     * @param pairingRequestId
     *            The unique id for a pairing request
     * @return A PairingStatus object
     * @throws RequestError
     *             Thrown when an exceptional condition is encountered
     */
    public PairingStatus getPairingStatus(String pairingRequestId) throws RequestError {
        final String endpoint = String.format("pairings/%s", pairingRequestId);

        try {
            JSONObject json = get(endpoint);
            return PairingStatus.fromJSON(json);
        } catch (Exception e) {
            throw new RequestError(e);
        }
    }

    /**
     * Initiate a login authentication request
     * 
     * @param pairingId
     *            The pairing id indicating to whom the request should be sent
     * @param terminalName
     *            The user-facing descriptive name for the terminal from which the request originates
     * @return An AuthenticationStatus object
     * @throws RequestError
     *             Thrown when an exceptional condition is encountered
     */
    public AuthenticationStatus authenticate(String pairingId, String terminalName) throws RequestError {
        return authenticate(pairingId, terminalName, null);
    }

    /**
     * Initiate an authentication request
     * 
     * @param pairingId
     *            The pairing id indicating to whom the request should be sent
     * @param terminalName
     *            The user-facing descriptive name for the terminal from which the request originates
     * @param actionName
     *            The user-facing descriptive name for the action which is being authenticated
     * @return An AuthenticationStatus object
     * @throws RequestError
     *             Thrown when an exceptional condition is encountered
     */
    public AuthenticationStatus authenticate(String pairingId, String terminalName,
                                             String actionName) throws RequestError {
        final String endpoint = "authentication_requests/initiate";

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("pairing_id", pairingId));
        params.add(new BasicNameValuePair("terminal_name", terminalName));
        if (actionName != null && actionName.length() > 0) {
            params.add(new BasicNameValuePair("action_name", actionName));
        }

        try {
            JSONObject json = post(endpoint, params);
            return AuthenticationStatus.fromJSON(json);
        } catch (Exception e) {
            throw new RequestError(e);
        }
    }

    /**
     * Retrieve status information for an authentication request
     * 
     * @param authenticationRequestId
     *            The authentication request ID
     * @return An AuthenticationStatus object
     * @throws RequestError
     *             Thrown when an exceptional condition is encountered
     */
    public AuthenticationStatus getAuthenticationStatus(String authenticationRequestId)
            throws RequestError {
        final String endpoint = String.format("authentication_requests/%s", authenticationRequestId);

        try {
            JSONObject json = get(endpoint);
            return AuthenticationStatus.fromJSON(json);
        } catch (Exception e) {
            throw new RequestError(e);
        }
    }

    private JSONObject get(String endpoint) throws Exception {
        URI uri = new URIBuilder().setScheme(URI_SCHEME).setHost(URI_HOST)
                                  .setPath(URI_BASE + endpoint).build();
        HttpGet get = new HttpGet(uri);
        consumer.sign(get);
        return httpClient.execute(get, jsonHandler);
    }

    private JSONObject post(String endpoint, List<NameValuePair> params) throws Exception {
        URI uri = new URIBuilder().setScheme(URI_SCHEME).setHost(URI_HOST)
                                  .setPath(URI_BASE + endpoint).build();
        HttpPost post = new HttpPost(uri);
        if (params != null && params.size() > 0) {
            post.setEntity(new UrlEncodedFormEntity(params));
        }

        consumer.sign(post);

        return httpClient.execute(post, jsonHandler);
    }

    private static ResponseHandler<JSONObject> jsonHandler = new ResponseHandler<JSONObject>() {

        @Override
        public JSONObject handleResponse(HttpResponse response) throws ClientProtocolException,
                IOException {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() >= 300) {
                throw new HttpResponseException(statusLine.getStatusCode(),
                                                statusLine.getReasonPhrase());
            }

            HttpEntity entity = response.getEntity(); // TODO: check entity == null
            String json = EntityUtils.toString(entity);

            try {
                return (JSONObject) new JSONTokener(json).nextValue();
            } catch (JSONException e) {
                throw new ClientProtocolException("Could not interpret response as JSON", e);
            }
        }
    };

    private static final String URI_SCHEME = "https";
    private static final String URI_HOST = "api.toopher.com";
    private static final String URI_BASE = "/v1/";

    private final HttpClient httpClient;
    private final OAuthConsumer consumer;
}
