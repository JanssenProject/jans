/*
 * oxUtil is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.xdi.util.init;


/**
 * Allow class to avoid parallel initializations
 * 
 * @author Yuriy Movchan 11/14/2014
 */
public abstract class Initializable {

	private static Object lock = new Object();
    
    private boolean initialized = false;

    public void init() {
    	if (!this.initialized) {
			synchronized (lock) {
                if (!this.initialized) {
                    initInternal();
                    this.initialized = true;
                }
			}
    	}
    }
    
    public synchronized void reinit() {
    	initInternal();
        this.initialized = true;
    }
    
    protected abstract void initInternal();
}
