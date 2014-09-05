/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import org.xdi.oxauth.model.uma.RptStatusRequest;
import org.xdi.oxauth.model.uma.RptStatusResponse;
import org.xdi.oxauth.model.uma.UmaConstants;

/**
 * The endpoint at which the host requests the status of an RPT presented to it by a requester.
 * The endpoint is RPT introspection profile implementation defined here:
 * http://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-bearer-token-profile
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10/23/2012
 */
public interface RptStatusService {

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public RptStatusResponse requestRptStatus(@HeaderParam("Authorization") String authorization,
                                              RptStatusRequest tokenStatusRequest);

}
