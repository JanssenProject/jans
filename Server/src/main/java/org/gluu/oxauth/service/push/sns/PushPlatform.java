package org.gluu.oxauth.service.push.sns;

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
	MPNS;

}
