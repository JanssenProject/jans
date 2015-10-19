package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.xdi.oxd.rp.client.demo.shared.TokenDetails;

public interface ServiceAsync {
    void getAuthorizationUrl(AsyncCallback<String> async);

    void getTokenDetails(String href, AsyncCallback<TokenDetails> async);
}
