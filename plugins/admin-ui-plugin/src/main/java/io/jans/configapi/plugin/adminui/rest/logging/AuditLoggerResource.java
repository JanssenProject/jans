package io.jans.configapi.plugin.adminui.rest.logging;

import io.jans.configapi.plugin.adminui.utils.ErrorResponse;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/admin-ui/logging")
public class AuditLoggerResource {

    static final String AUDIT = "/audit";

    @Inject
    Logger log;

    @POST
    @Path(AUDIT)
    @Produces(MediaType.APPLICATION_JSON)
    public Response auditLogging(@Valid @NotNull Map<String, Object> loggingRequest) {
        try {
            log.info(loggingRequest.toString());
            return Response.ok("{'status': 'success'}").build();
        } catch (Exception e) {
            log.error(ErrorResponse.AUDIT_LOGGING_ERROR.getDescription(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
