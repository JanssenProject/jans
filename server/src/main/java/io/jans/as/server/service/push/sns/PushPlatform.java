/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.push.sns;

/**
 * Platforms supported AWS SNS
 *
 * @author Yuriy Movchan Date: 08/31/2017
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
	MPNS

}
