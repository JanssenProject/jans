package org.gluu.service.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Important : keep it weld free. It's reused by oxd !
 *
 * @author yuriyz
 */
public class RedisClusterProvider extends AbstractRedisProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RedisClusterProvider.class);

    private JedisCluster pool;

    public RedisClusterProvider(RedisConfiguration redisConfiguration) {
        super(redisConfiguration);
    }

    public void create() {
        try {
            LOG.debug("Starting RedisClusterProvider ... configuration:" + getRedisConfiguration());

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(1000);
            poolConfig.setMinIdle(2);

            pool = new JedisCluster(hosts(getRedisConfiguration().getServers()), poolConfig);

            testConnection();
            LOG.debug("RedisClusterProvider started.");
        } catch (Exception e) {
            LOG.error("Failed to start RedisClusterProvider.");
            throw new IllegalStateException("Error starting RedisClusterProvider", e);
        }
    }

    public static Set<HostAndPort> hosts(String servers) {
        final String[] serverWithPorts = StringUtils.split(servers.trim(), ",");

        Set<HostAndPort> set = new HashSet<HostAndPort>();
        for (String serverWithPort : serverWithPorts) {
            final String[] split = serverWithPort.trim().split(":");
            String host = split[0];
            int port = Integer.parseInt(split[1].trim());
            set.add(new HostAndPort(host, port));
        }
        return set;
    }

    public void destroy() {
        LOG.debug("Destroying RedisClusterProvider");

        try {
            pool.close();
        } catch (IOException e) {
            LOG.error("Failed to destroy RedisClusterProvider", e);
            return;
        }

        LOG.debug("Destroyed RedisClusterProvider");
    }

    @Override
    public JedisCluster getDelegate() {
        return pool;
    }

    @Override
    public Object get(String key) {
        byte[] value = pool.get(key.getBytes());
        Object deserialized = null;
        if (value != null && value.length > 0) {
            deserialized = SerializationUtils.deserialize(value);
        }
        return deserialized;
    }

    @Override
    public void put(int expirationInSeconds, String key, Object object) {
        String status = pool.setex(key.getBytes(), expirationInSeconds, SerializationUtils.serialize((Serializable) object));
        LOG.trace("put - key: " + key + ", status: " + status);
    }

    @Override
    public void put(String key, Object object) {
        String status = pool.set(key.getBytes(), SerializationUtils.serialize((Serializable) object));
        LOG.trace("put - key: " + key + ", status: " + status);
    }

    @Override
    public void remove(String key) {
        Long entriesRemoved = pool.del(key.getBytes());
        LOG.trace("remove - key: " + key + ", entriesRemoved: " + entriesRemoved);
    }

    @Override
    public void clear() {
        LOG.trace("clear not allowed for cluster deployments");
    }
}
