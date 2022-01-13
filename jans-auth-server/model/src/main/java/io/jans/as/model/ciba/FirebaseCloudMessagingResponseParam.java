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
public interface FirebaseCloudMessagingResponseParam {

    /**
     * Unique ID (number) identifying the multicast message.
     */
    String MULTICAST_ID = "multicast_id";

    /**
     * Number of messages that were processed without an error.
     */
    String SUCCESS = "success";

    /**
     * Number of messages that could not be processed.
     */
    String FAILURE = "failure";

    /**
     * Array of objects representing the status of the messages processed.
     * The objects are listed in the same order as the request (i.e., for each registration ID in the request,
     * its result is listed in the same index in the response).
     */
    String RESULTS = "results";

    /**
     * String specifying a unique ID for each successfully processed message.
     */
    String MESSAGE_ID = "message_id";
}