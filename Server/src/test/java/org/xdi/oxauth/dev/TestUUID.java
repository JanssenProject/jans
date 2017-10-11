package org.xdi.oxauth.dev;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TestUUID {
	
	public static ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
	public static long errors_count = 0;
	public static long done = 0;
	
	public static void main(String[] args) throws InterruptedException {
		for (int i = 0; i < 100; i++)
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				for (int i = 0; i < 10000; i++ ) {
					String uuid = UUID.randomUUID().toString();
					if (map.contains(uuid)) {
						errors_count++;
					} else {
						map.put(uuid, uuid);
					}
				}
				System.out.println("Done");
				done++;
			}
		}).start();
		
		while (true) {
			Thread.sleep(5*1000);
			if (done == 100) {
				break;
			}
		}
		
		System.out.println(map.size());
		System.out.println(errors_count);
	}

}
