package org.xdi.oxd.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yuriyz
 */
public abstract class ObjectPool<T> {

    private long expirationTime;

    protected final Map<T, Long> locked = new HashMap<T, Long>();
    protected final Map<T, Long> unlocked = new HashMap<T, Long>();

    public ObjectPool(int expirationInSeconds) {
        expirationTime = expirationInSeconds * 1000;
    }

    protected abstract T create() throws IOException;

    public abstract boolean validate(T o);

    public abstract void expire(T o);

    public synchronized T checkOut() throws IOException {
        long now = System.currentTimeMillis();
        T t = null;
        if (unlocked.size() > 0) {
            for (T obj : unlocked.keySet()) {
                t = obj;
                if ((now - unlocked.get(t)) > expirationTime) {
                    expire(t);
                    t = null;
                } else {
                    if (validate(t)) {
                        unlocked.remove(t);
                        locked.put(t, now);
                        return (t);
                    } else {
                        expire(t);
                        t = null;
                    }
                }
            }
        }
        // no objects available, create a new one
        t = create();
        locked.put(t, now);
        return t;
    }

    public synchronized void checkIn(T t) {
        if (t == null) {
            return;
        }
        locked.remove(t);
        unlocked.put(t, System.currentTimeMillis());
    }
}