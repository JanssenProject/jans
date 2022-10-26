/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.push.sns;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides operations to send AWS SNS push messages
 *
 * @author Yuriy Movchan Date: 08/31/2017
 */
@Stateless
@Named
public class PushSnsService {

    @Inject
    private EncryptionService encryptionService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    public AmazonSNS createSnsClient(String accessKey, String secretKey, String region) {
        String decryptedAccessKey = encryptionService.decrypt(accessKey, true);
        String decryptedSecretKey = encryptionService.decrypt(secretKey, true);

        BasicAWSCredentials credentials = new BasicAWSCredentials(decryptedAccessKey, decryptedSecretKey);
        return AmazonSNSClientBuilder.standard().withRegion(Regions.fromName(region)).withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
    }

    public String createPlatformArn(AmazonSNS snsClient, String platformApplicationArn, String token, User user) {
        CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
        platformEndpointRequest.setPlatformApplicationArn(platformApplicationArn);
        platformEndpointRequest.setToken(token);

        String customUserData = getCustomUserData(user);
        platformEndpointRequest.setCustomUserData(customUserData);

        CreatePlatformEndpointResult platformEndpointResult = snsClient.createPlatformEndpoint(platformEndpointRequest);

        return platformEndpointResult.getEndpointArn();
    }

    public String getCustomUserData(User user) {
        return String.format("Issuer: %s, user: %s, date: %s", appConfiguration.getIssuer(), user.getUserId(),
                ldapEntryManager.encodeTime(user.getDn(), new Date()));
    }

    public PublishResult sendPushMessage(AmazonSNS snsClient, PushPlatform platform, String targetArn, Map<String, Object> customAppMessageMap, Map<String, MessageAttributeValue> messageAttributes) throws IOException {
        Map<String, Object> appMessageMap = new HashMap<String, Object>();

        if (platform == PushPlatform.GCM) {
            appMessageMap.put("collapse_key", "single");
            appMessageMap.put("delay_while_idle", true);
            appMessageMap.put("time_to_live", 30);
            appMessageMap.put("dry_run", false);
        }

        if (customAppMessageMap != null) {
            appMessageMap.putAll(customAppMessageMap);
        }

        String message = ServerUtil.asJson(appMessageMap);

        return sendPushMessage(snsClient, platform, targetArn, message, messageAttributes);
    }

    public PublishResult sendPushMessage(AmazonSNS snsClient, PushPlatform platform, String targetArn, String message,
                                         Map<String, MessageAttributeValue> messageAttributes) throws IOException {
        Map<String, String> messageMap = new HashMap<String, String>();
        messageMap.put(platform.name(), message);
        message = ServerUtil.asJson(messageMap);

        PublishRequest publishRequest = new PublishRequest();
        publishRequest.setMessageStructure("json");

        if (messageAttributes != null) {
            publishRequest.setMessageAttributes(messageAttributes);
        }

        publishRequest.setTargetArn(targetArn);
        publishRequest.setMessage(message);

        return snsClient.publish(publishRequest);
    }
}
