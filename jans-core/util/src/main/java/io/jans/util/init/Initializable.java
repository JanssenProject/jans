/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.util.init;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Allow class to avoid parallel initializations
 *
 * @author Yuriy Movchan 11/14/2014
 */
public abstract class Initializable {

    private final ReentrantLock lock = new ReentrantLock();

    private boolean initialized = false;

    public void init() {
        if (!this.initialized) {
            lock.lock();
            try {
                if (!this.initialized) {
                    initInternal();
                    this.initialized = true;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public synchronized void reinit() {
        initInternal();
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    protected abstract void initInternal();

}
