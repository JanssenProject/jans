package org.xdi.oxd.license.admin.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public abstract class SuccessCallback<T> implements AsyncCallback<T> {

    private static final Logger LOGGER = Logger.getLogger(SuccessCallback.class.getName());

    @Override
    public void onFailure(Throwable caught) {
        LOGGER.log(Level.FINE, caught.getMessage(), caught);
    }
}
