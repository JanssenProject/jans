package org.gluu.service.cache;

import java.util.ArrayList;

import redis.clients.jedis.params.Params;
import redis.clients.util.SafeEncoder;

/**
 * Taken from here
 * https://github.com/xetorthio/jedis/blob/master/src/main/java/redis/clients/jedis/params/set/SetParams.java
 */
public final class RedisSetParams extends Params {

    private static final String XX = "xx";
    private static final String NX = "nx";
    private static final String PX = "px";
    private static final String EX = "ex"; // seconds

    private RedisSetParams() {
    }

    public static RedisSetParams setParams() {
        return new RedisSetParams();
    }

    /**
     * Set the specified expire time, in seconds.
     *
     * @param secondsToExpire
     * @return SetParams
     */
    public RedisSetParams ex(int secondsToExpire) {
        addParam(EX, secondsToExpire);
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param millisecondsToExpire
     * @return SetParams
     */
    public RedisSetParams px(long millisecondsToExpire) {
        addParam(PX, millisecondsToExpire);
        return this;
    }

    /**
     * Only set the key if it does not already exist.
     *
     * @return SetParams
     */
    public RedisSetParams nx() {
        addParam(NX);
        return this;
    }

    /**
     * Only set the key if it already exist.
     *
     * @return SetParams
     */
    public RedisSetParams xx() {
        addParam(XX);
        return this;
    }

    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<byte[]>();
        for (byte[] arg : args) {
            byteParams.add(arg);
        }

        if (contains(NX)) {
            byteParams.add(SafeEncoder.encode(NX));
        }
        if (contains(XX)) {
            byteParams.add(SafeEncoder.encode(XX));
        }

        if (contains(EX)) {
            byteParams.add(SafeEncoder.encode(EX));
            byteParams.add(SafeEncoder.encode(String.valueOf(getParam(EX))));
        }
        if (contains(PX)) {
            byteParams.add(SafeEncoder.encode(PX));
            byteParams.add(SafeEncoder.encode(String.valueOf(getParam(PX))));
        }

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
