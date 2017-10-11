package org.xdi.oxauth.util;

import org.gluu.oxnotify.client.NotifyClientFactory;
import org.gluu.oxnotify.client.NotifyClientService;
import org.gluu.oxnotify.client.NotifyMetadataClientService;
import org.gluu.oxnotify.model.NotificationResponse;
import org.gluu.oxnotify.model.NotifyMetadata;
import org.gluu.oxnotify.model.RegisterDeviceResponse;

public class TestClient {

	public static void main(String[] args) {
		NotifyMetadataClientService mService = NotifyClientFactory.instance().createMetaDataConfigurationService("https://api.gluu.org");
		NotifyMetadata notifyMetadata = mService.getMetadataConfiguration();
		System.out.println(notifyMetadata);
		
//		NotifyClientService client = NotifyClientFactory.instance().createPoolledNotifyService(notifyMetadata);
//		String authorization = NotifyClientFactory.getAuthorization("36WH2JiexBOoAIBP", "ueqsU2Dc7m3r4HmLz4M79DpzzCNqTfek");
//		RegisterDeviceResponse response = client.registerDevice(authorization, "nPkgqXNm6EUDkEdBDNeLTQ2FLvp3ZGjh0dZV98PolOUapqPaI9e2D-i_QDsq-Kb-HbCb2tJ5aSc4if7Rk0-Iww", "Test!!!");
//		System.out.println(response);
//		
//		String deviceArn = "arn:aws:sns:us-west-2:989705443609:endpoint/GCM/super_gluu_gcm/6bf94ccc-bcc5-3dc3-b4fa-55e27e0bf221";
//		
//		NotificationResponse response2 = client.sendNotification(authorization, deviceArn, "Test");
//		System.out.println(response2);
	}

}
