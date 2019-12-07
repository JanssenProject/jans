/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public interface FirebaseCloudMessagingRequestParam {

    /**
     * This parameter specifies the recipient of a message.
     * The value can be a device's registration token, a device group's notification key, or a single topic.
     */
    String TO = "to";

    /**
     * This parameter specifies the predefined, user-visible key-value pairs of the notification payload.
     */
    String NOTIFICATION = "notification";

    /**
     * The notification's title.
     * This field is not visible on iOS phones and tablets.
     */
    String TITLE = "title";

    /**
     * The notification's body text.
     */
    String BODY = "body";

    /**
     * The action associated with a user click on the notification.
     */
    String CLICK_ACTION = "click_action";
}