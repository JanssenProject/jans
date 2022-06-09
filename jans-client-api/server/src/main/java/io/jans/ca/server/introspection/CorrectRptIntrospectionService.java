package io.jans.ca.server.introspection;

import io.jans.as.model.uma.UmaConstants;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import jakarta.ws.rs.*;

/**
 * @author yuriyz
 */
public interface CorrectRptIntrospectionService {
    @POST
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    CorrectRptIntrospectionResponse requestRptStatus(@HeaderParam("Authorization") String authorization,
                                                     @FormParam("token") String rptAsString,
                                                     @FormParam("token_type_hint") String tokenTypeHint);
}
