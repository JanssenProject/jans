package org.xdi.oxd.rp.client.demo.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/10/2015
 */
@RemoteServiceRelativePath("rpService")
public interface Service extends RemoteService {

    String getAuthorizationUrl();
}
