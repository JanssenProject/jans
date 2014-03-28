package org.xdi.oxauth.service.uma;

import org.xdi.oxauth.model.common.AbstractToken;
import org.xdi.oxauth.model.common.uma.UmaRPT;
import org.xdi.util.INumGenerator;

import java.util.Date;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/02/2013
 */

public abstract class AbstractRPTManager implements IRPTManager {

//    public String getRequesterPermissionTokenKey(String userId, String clientId, String amHost) {
//		return new StringBuilder("_rp_").append(userId.hashCode())
//				.append("_client_").append(clientId.hashCode())
//				.append("_am_host_").append(amHost.hashCode()).toString();
//	}

    public UmaRPT createRPT(AbstractToken authorizationApiToken, String userId, String clientId, String amHost) {
   		String token = UUID.randomUUID().toString() + "/" + INumGenerator.generate(8);

   		return new UmaRPT(token, new Date(), authorizationApiToken.getExpirationDate(), userId, clientId, amHost);
   	}
}
