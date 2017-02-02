package org.xdi.oxauth.dev;

import com.google.common.collect.Lists;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.xdi.oxauth.model.common.SessionState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author yuriyz on 02/02/2017.
 */
public class MemcacheManual {

    public static void main(String[] args) throws IOException {
        MemcachedClient client = new MemcachedClient(new DefaultConnectionFactory(100000, 32768),
                Lists.newArrayList(new InetSocketAddress("localhost", 11211)));

        client.set("myKey", 30, testState());

        System.out.println(client.get("myKey"));

        sleep(20);
        System.out.println(client.get("myKey"));

        client.set("myKey", 30, testState());

        sleep(12);
        System.out.println(client.get("myKey"));

        sleep(12);
        System.out.println(client.get("myKey"));

        client.shutdown();
    }

    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private static SessionState testState() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("mapKey", "mapValue");

        SessionState state = new SessionState();
        state.setUserDn("userDn");
        state.setId(UUID.randomUUID().toString());
        state.setLastUsedAt(new Date());
        state.setSessionAttributes(map);
        return state;
    }
}
