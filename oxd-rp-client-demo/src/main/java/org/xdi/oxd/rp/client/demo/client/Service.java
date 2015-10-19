package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.xdi.oxd.rp.client.demo.shared.TokenDetails;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */
@RemoteServiceRelativePath("rpService")
public interface Service extends RemoteService {

    String getAuthorizationUrl();

    TokenDetails getTokenDetails(String href);
}
