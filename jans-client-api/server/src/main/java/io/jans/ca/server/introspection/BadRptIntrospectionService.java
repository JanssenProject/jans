package io.jans.ca.server.introspection;

import io.jans.as.model.uma.UmaConstants;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;

/**
 * @author yuriyz
 */
public interface BadRptIntrospectionService {

    @POST
    @Produces({UmaConstants.JSON_MEDIA_TYPE})
    BadRptIntrospectionResponse requestRptStatus(@HeaderParam("Authorization") String authorization,
                                                 @FormParam("token") String rptAsString,
                                                 @FormParam("token_type_hint") String tokenTypeHint);
}
