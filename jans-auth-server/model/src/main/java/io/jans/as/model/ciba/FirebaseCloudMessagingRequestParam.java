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
public class FirebaseCloudMessagingRequestParam {

    private FirebaseCloudMessagingRequestParam() {
    }

    /**
     * This parameter specifies the recipient of a message.
     * The value can be a device's registration token, a device group's notification key, or a single topic.
     */
    public static final String TO = "to";

    /**
     * This parameter specifies the predefined, user-visible key-value pairs of the notification payload.
     */
    public static final String NOTIFICATION = "notification";

    /**
     * The notification's title.
     * This field is not visible on iOS phones and tablets.
     */
    public static final String TITLE = "title";

    /**
     * The notification's body text.
     */
    public static final String BODY = "body";

    /**
     * The action associated with a user click on the notification.
     */
    public static final String CLICK_ACTION = "click_action";
}