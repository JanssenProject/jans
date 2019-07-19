package org.gluu.oxd.server.introspection;

import org.gluu.oxauth.model.uma.UmaConstants;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

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
