/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.fcm;

import io.jans.as.client.BaseResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static io.jans.as.model.ciba.FirebaseCloudMessagingResponseParam.FAILURE;
import static io.jans.as.model.ciba.FirebaseCloudMessagingResponseParam.MESSAGE_ID;
import static io.jans.as.model.ciba.FirebaseCloudMessagingResponseParam.MULTICAST_ID;
import static io.jans.as.model.ciba.FirebaseCloudMessagingResponseParam.RESULTS;
import static io.jans.as.model.ciba.FirebaseCloudMessagingResponseParam.SUCCESS;

/**
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
public class FirebaseCloudMessagingResponse extends BaseResponse {

    private static final Logger LOG = Logger.getLogger(FirebaseCloudMessagingResponse.class);

    private Long multicastId;
    private int success;
    private int failure;
    private List<Result> results;

    public FirebaseCloudMessagingResponse(Response clientResponse) {
        super(clientResponse);

        setHeaders(clientResponse.getMetadata());
        injectDataFromJson(entity);
    }

    public void injectDataFromJson(String json) {
        if (StringUtils.isBlank(json)) {
            return;
        }
        try {
            JSONObject jsonObj = new JSONObject(json);

            if (jsonObj.has(MULTICAST_ID)) {
                multicastId = jsonObj.getLong(MULTICAST_ID);
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

    class Result {
        private final String messageId;

        public Result(String messageId) {
            this.messageId = messageId;
        }
    }
} 