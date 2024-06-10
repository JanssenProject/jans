/*
 * Copyright [2024] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.lock.test.dev;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class TestPubSubRedis {

	public static void main(String args[]) throws Exception {
		List<ListenerRedisSub> pubSubList = new ArrayList<>();

		ExecutorService executorService = Executors.newCachedThreadPool(daemonThreadFactory());

		for (int i = 0; i < 1; i++) {
			Jedis jedisClientListener = new Jedis("192.168.1.151", 6379);
			ListenerRedis listener = new ListenerRedis(jedisClientListener, i);
			pubSubList.add(listener.getListenerRedisSub());
			Future<?> future = executorService.submit(listener);
			System.out.println(future);
		}

		for (int i = 0; i < 1; i++) {
			Jedis jedisClientNotifier = new Jedis("192.168.1.151", 6379);
			executorService.execute(new NotifierRedis(jedisClientNotifier));
		}
		
		Thread.sleep(10*1000);
		Random random = new Random();
		for (int i = 0; i < 1; i++) {
			int idx = random.nextInt(pubSubList.size());
			ListenerRedisSub listenerRedisSub = pubSubList.get(idx);
			pubSubList.remove(idx);

			listenerRedisSub.unsubscribe();
			Thread.sleep(2*1000);
		}
		
		Thread.sleep(24*60*60*1000);
	}


	public static ThreadFactory daemonThreadFactory() {
        return new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("PubSubThread");
                thread.setDaemon(true);
                return thread;
            }
        };
    }
}

class ListenerRedis implements Runnable  {

	private Jedis jedisClient;
	private int idx;
	private ListenerRedisSub listenerRedisSub;

	ListenerRedis(Jedis jedisClient, int idx) throws SQLException {
		this.jedisClient = jedisClient;
		this.idx = idx;
		this.listenerRedisSub = new ListenerRedisSub(idx);
	}

	public void run() {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        currentThread.setName("PubSubListener-" + "mymessage" + idx);
        try {
    		jedisClient.subscribe(listenerRedisSub, "mymessage");
        } finally {
            currentThread.setName(oldName);
        }
	}

	public ListenerRedisSub getListenerRedisSub() {
		return listenerRedisSub;
	}
	
}

class NotifierRedis implements Runnable {
	private Jedis jedisClient;
	private static long total = 0;

	public NotifierRedis(Jedis jedisClient) {
		this.jedisClient = jedisClient;
	}

	public void run() {
		while (true) {
			try {
				jedisClient.publish("mymessage", Base64.getEncoder().encodeToString(String.valueOf(new Random().nextInt(31*6)).getBytes()) + "'");
				total++;
				
				if (total % 1000 == 0) {
					System.out.printf("-:\t %d\t: Send notifications count\n", total);
				}
				Thread.sleep(1);
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}


}

class ListenerRedisSub extends JedisPubSub {
	private long total = 0;
	private int idx;

	ListenerRedisSub( int idx) {
		this.idx = idx;
	}

    @Override
    public void onMessage(String channel, String message) {
		total++;
//        System.out.println("received message from channel:" + channel + " with value:" +message);
		if (total % 1000 == 0) {
			System.out.printf("%d:\t %d\t: Got messgae\n", idx, total);
		}
    }
 
    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        System.out.println("Client is Subscribed to channel : "+ channel);
        System.out.println("Client is Subscribed to "+ subscribedChannels + " no. of channels");
    }
         
    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        System.out.println("Client is Unsubscribed from channel : "+ channel);
        System.out.println("Client is Subscribed to "+ subscribedChannels + " no. of channels");
    }
         
}
