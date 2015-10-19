package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ServiceAsync {
    void getAuthorizationUrl(AsyncCallback<String> async);
}
