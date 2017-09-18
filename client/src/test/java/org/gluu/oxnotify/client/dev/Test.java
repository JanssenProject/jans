package org.gluu.oxnotify.client.dev;

import org.gluu.oxnotify.client.NotifyClientFactory;
import org.gluu.oxnotify.client.NotifyClientService;
import org.gluu.oxnotify.client.NotifyMetadataClientService;
import org.gluu.oxnotify.model.NotifyMetadata;
import org.gluu.oxnotify.model.RegisterDeviceResponse;

public class Test {

	public static void main(String[] args) throws Exception {
        NotifyClientFactory client = NotifyClientFactory.instance();

        NotifyMetadataClientService notifyMetadataClientService = client.createMetaDataConfigurationService("https://ce-prev-version.gluu.org:9443/oxnotify/seam/resource/rest/oxnotify/broker-configuration");
		NotifyMetadata notifyMetadata = notifyMetadataClientService.getMetadataConfiguration();

		NotifyClientService notifyClientService = client.createNotifyService(notifyMetadata);
		
		String authorization = "";
		String token = "";
		String userData = "Test user data";
		String endpoint = "";
		String message = "";

		RegisterDeviceResponse registerDeviceResponse = notifyClientService.registerDevice(authorization, token, userData);
		System.out.println(registerDeviceResponse);

		notifyClientService.sendNotification(authorization, endpoint, message);

	}

}
