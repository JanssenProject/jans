package io.jans.as.client.service;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 */
public interface StatService {
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    JsonNode stat(@HeaderParam("Authorization") String authorization,
                  @QueryParam("month") String month,
                  @QueryParam("start-month") String startMonth,
                  @QueryParam("end-month") String endMonth,
                  @QueryParam("format") String format);

    @GET
    String statOpenMetrics(@HeaderParam("Authorization") String authorization, @QueryParam("month") String month, @QueryParam("format") String format);

    @POST
    @Produces({MediaType.APPLICATION_JSON})
    JsonNode statPost(@HeaderParam("Authorization") String authorization,
                      @FormParam("month") String month,
                      @FormParam("start-month") String startMonth,
                      @FormParam("end-month") String endMonth,
                      @FormParam("format") String format);
}
