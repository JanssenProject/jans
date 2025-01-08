package io.jans.lock.service.ws.rs.stat;

import io.jans.service.security.api.ProtectedApi;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Provides server with basic statistic
 *
 * @author Yuriy Movchan Date: 12/02/2024
 */
@Dependent
@Path("/internal/stat")
public interface StatRestWebService {

    @GET
	@ProtectedApi(scopes = {"jans_stat"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response statGet(@HeaderParam("Authorization") String authorization,
                            @QueryParam("month") String months,
                            @QueryParam("start-month") String startMonth,
                            @QueryParam("end-month") String endMonth,
                            @QueryParam("format") String format);

    @POST
	@ProtectedApi(scopes = {"jans_stat"})
    @Produces(MediaType.APPLICATION_JSON)
    public Response statPost(@HeaderParam("Authorization") String authorization,
                             @FormParam("month") String months,
                             @FormParam("start-month") String startMonth,
                             @FormParam("end-month") String endMonth,
                             @FormParam("format") String format);
}
