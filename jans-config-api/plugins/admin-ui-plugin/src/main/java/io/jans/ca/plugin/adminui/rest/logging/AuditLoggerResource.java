package io.jans.ca.plugin.adminui.rest.logging;

import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Hidden
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
