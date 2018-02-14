package org.xdi.oxd.server.introspection;

import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

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
