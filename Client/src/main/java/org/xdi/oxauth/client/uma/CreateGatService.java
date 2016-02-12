package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.GatRequest;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the requester asks the AS to issue an GAT (authorized token for given scopes,
 * GAT stands for Gluu Access Token.)
 *
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 19/01/2016
 */

public interface CreateGatService {

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public RPTResponse createGAT(@HeaderParam("Authorization") String authorization,
                                 @HeaderParam("Host") String host,
                                 GatRequest request);

}
