package org.xdi.oxauth.uma.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.xdi.oxauth.model.uma.RptStatusRequest;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * The endpoint at which the host requests the status of an RPT presented to it by a requester.
 * The endpoint is RPT introspection profile implementation defined here:
 * http://docs.kantarainitiative.org/uma/draft-uma-core.html#uma-bearer-token-profile
 *
 * @author Yuriy Movchan
 * @author Yuriy Zabrovarnyy
 *         Date: 10/23/2012
 */
@Path("/host/status")
@Api(value="/host/status", description = "The endpoint at which the host requests the status of an RPT presented to it by a requester." +
        " The endpoint is RPT introspection profile implementation defined by UMA specification")
public interface RptStatusRestWebService {

    @POST
    @Consumes({UmaConstants.JSON_MEDIA_TYPE})
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    public Response requestRptStatus(@HeaderParam("Authorization") String authorization,
                                     RptStatusRequest tokenStatusRequest);

}