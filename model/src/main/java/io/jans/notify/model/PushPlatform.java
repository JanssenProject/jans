/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package io.jans.notify.model;

/**
 * Platforms supported AWS SNS
 *
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public enum PushPlatform {

	// Apple Push Notification Service
	APNS,
	// Sandbox version of Apple Push Notification Service
	APNS_SANDBOX,
	// Amazon Device Messaging
	ADM,
	// Google Cloud Messaging
	GCM,
	// Baidu CloudMessaging Service
	BAIDU,
	// Windows Notification Service
	WNS,
	// Microsoft Push Notificaion Service
	MPNS;

}
