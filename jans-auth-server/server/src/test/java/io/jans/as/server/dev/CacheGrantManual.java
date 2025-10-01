/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.dev;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.server.model.common.CacheGrant;
import io.jans.as.common.model.session.SessionId;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author yuriyz
 * @version August 9, 2017
 */
public class CacheGrantManual {

    private static final int CLIENT_COUNT = 10;
    private static final Random RANDOM = new Random();

    private static MemcachedClient createClients() throws IOException {
        return new MemcachedClient(new DefaultConnectionFactory(100, 32768),
                Lists.newArrayList(new InetSocketAddress("localhost", 11211)));
    }

    private static int random() {
        int min = 0;
        int max = CLIENT_COUNT;
        return RANDOM.nextInt(max - min + 1) + min;
    }

    public static void main(String[] args) throws IOException {
        final MemcachedClient client = createClients();
        final ExecutorService executorService = Executors.newFixedThreadPool(1000, daemonThreadFactory());

        int count = 10000;
        final long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            final int key = i;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    // MemcachedClient client = clients.get(random);

                    Object toPut = testGrant();
                    // Object toPut = UUID.randomUUID().toString();

                    OperationFuture<Boolean> set = null;
                    for (int j = 0; j < 3; j++) {
                        set = client.set(Integer.toString(key), 60, toPut);
                    }

                    OperationStatus status = set.getStatus(); // block

                    System.out.println(
                            " key: " + key + ", time: " + (new Date().getTime() - start) + "ms, set: " + status);

                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {

                            int random = random();
                            if (random % 3 == 0) {
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            Object get = client.get(Integer.toString(key));
                            System.out.println("GET key: " + key + " result: " + get);
                        }
                    });
                }
            });
        }

        sleep(40);
        // System.out.println(client.get("myKey"));
        //
        // client.set("myKey", 30, testState());
        //
        // sleep(12);
        // System.out.println(client.get("myKey"));
        //
        // sleep(12);
        // System.out.println(client.get("myKey"));
        client.shutdown();

    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static ThreadFactory daemonThreadFactory() {
        return new ThreadFactory() {
            public Thread newThread(Runnable p_r) {
                Thread thread = new Thread(p_r);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    private static SessionId testState() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("mapKey", "mapValue");

        SessionId state = new SessionId();
        state.setUserDn("userDn");
        state.setId(UUID.randomUUID().toString());
        state.setLastUsedAt(new Date());
        state.setSessionAttributes(map);
        return state;
    }

    private static Client testClient() {
        Client client = new Client();
        client.setClientId(UUID.randomUUID().toString());
        return client;
    }

    private static User testUser() {
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        return user;
    }

    private static CacheGrant testGrant() {
        CacheGrant grant = new CacheGrant();
        grant.setAcrValues("basic");
        grant.setAuthenticationTime(new Date());
        grant.setAuthorizationCodeString(UUID.randomUUID().toString());
        grant.setClient(testClient());
        grant.setGrantId(UUID.randomUUID().toString());
        grant.setNonce(UUID.randomUUID().toString());
        grant.setScopes(Sets.newHashSet("openid"));
        grant.setUser(testUser());
        return grant;
    }
}
