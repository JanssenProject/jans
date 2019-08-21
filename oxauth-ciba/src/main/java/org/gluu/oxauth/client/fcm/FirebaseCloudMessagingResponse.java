/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.fcm;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.ClientResponse;
import org.gluu.oxauth.client.BaseResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.gluu.oxauth.model.ciba.FirebaseCloudMessagingResponseParam.*;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class FirebaseCloudMessagingResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(FirebaseCloudMessagingResponse.class);

    private String multicastId;
    private int success;
    private int failure;
    private List<Result> results;

    public FirebaseCloudMessagingResponse(ClientResponse<String> clientResponse) {
        super(clientResponse);

        String entity = clientResponse.getEntity(String.class);
        setEntity(entity);
        setHeaders(clientResponse.getMetadata());
        injectDataFromJson(entity);
    }

    public void injectDataFromJson(String p_json) {
        if (StringUtils.isNotBlank(p_json)) {
            try {
                JSONObject jsonObj = new JSONObject(p_json);

                if (jsonObj.has(MULTICAST_ID)) {
                    multicastId = jsonObj.getString(MULTICAST_ID);
                }
                if (jsonObj.has(SUCCESS)) {
                    success = jsonObj.getInt(SUCCESS);
                }
                if (jsonObj.has(FAILURE)) {
                    failure = jsonObj.getInt(FAILURE);
                }
                if (jsonObj.has(RESULTS)) {
                    results = new ArrayList<>();
                    JSONArray resultsJsonArray = jsonObj.getJSONArray(RESULTS);

                    for (int i = 0; i < resultsJsonArray.length(); i++) {
                        JSONObject resultJsonObject = resultsJsonArray.getJSONObject(i);

                        if (resultJsonObject.has(MESSAGE_ID)) {
                            Result result = new Result(resultJsonObject.getString(MESSAGE_ID));
                            results.add(result);
                        }
                    }
                }
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    class Result {
        private String messageId;

        public Result(String messageId) {
            this.messageId = messageId;
        }
    }
} 