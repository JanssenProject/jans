/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class FirebaseCloudMessagingResponseParam {

    private FirebaseCloudMessagingResponseParam() {
    }

    /**
     * Unique ID (number) identifying the multicast message.
     */
    public static final String MULTICAST_ID = "multicast_id";

    /**
     * Number of messages that were processed without an error.
     */
    public static final String SUCCESS = "success";

    /**
     * Number of messages that could not be processed.
     */
    public static final String FAILURE = "failure";

    /**
     * Array of objects representing the status of the messages processed.
     * The objects are listed in the same order as the request (i.e., for each registration ID in the request,
     * its result is listed in the same index in the response).
     */
    public static final String RESULTS = "results";

    /**
     * public static final String specifying a unique ID for each successfully processed message.
     */
    public static final String MESSAGE_ID = "message_id";
}