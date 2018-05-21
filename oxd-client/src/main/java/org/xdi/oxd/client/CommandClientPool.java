package org.xdi.oxd.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author yuriyz
 */
public class CommandClientPool extends ObjectPool<CommandClient> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CommandClientPool.class);

    private final ConcurrentSkipListSet<Integer> loggerNames = new ConcurrentSkipListSet<>();
    private final String host;
    private final int port;

    public CommandClientPool(int expirationInSeconds, String host, int port) {
        super(expirationInSeconds);
        this.host = host;
        this.port = port;
    }

    @Override
    protected CommandClient create() throws IOException {
        CommandClient client = new CommandClient(host, port);
        client.setNameForLogger(nextLoggerName());
        return client;
    }

    @Override
    public boolean validate(CommandClient o) {
        return o.isValid();
    }

    @Override
    public void expire(CommandClient o) {
        try {
            loggerNames.remove(o.getNameForLogger());
            locked.remove(o);
            unlocked.remove(o);
        } finally {
            CommandClient.closeQuietly(o);
        }
    }

    @Override
    public synchronized CommandClient checkOut() {
        try {
            return super.checkOut();
        } catch (IOException e) {
            LOG.error("Failed to check out command client.", e);
            return null;
        }
    }

    private synchronized int nextLoggerName() {
        for (int i = 1; i < 10000; i++) {
            if (!loggerNames.contains(i)) {
                loggerNames.add(i);
                return i;
            }
        }
        return -2;
    }
}
