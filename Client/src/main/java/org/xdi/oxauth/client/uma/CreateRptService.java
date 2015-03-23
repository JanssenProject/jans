/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the requester asks the AM to issue an RPT relating to
 * this requesting party, host, and AM.
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 */
public interface CreateRptService {

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public RPTResponse createRPT(@HeaderParam("Authorization") String authorization,
                                 @HeaderParam("Host") String host);

}