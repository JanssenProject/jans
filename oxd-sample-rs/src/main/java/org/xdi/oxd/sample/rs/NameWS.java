package org.xdi.oxd.sample.rs;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.xdi.oxd.rs.protect.Jackson;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/06/2016
 */

@Path("/name")
public class NameWS {

    private static final Logger LOG = Logger.getLogger(NameWS.class);

    private final List<String> names = Lists.newArrayList();

    @POST
    @Path("{name}")
    @Produces({"application/json"})
    public Response add(@PathParam("name") String name) {
        LOG.debug("Try to create/add name:" + name);
        names.add(name);
        return Response.ok().build();
    }

    @DELETE
    @Path("{name}")
    @Produces({"application/json"})
    public Response delete(@PathParam("name") String name) {
        LOG.debug("Try to remove name:" + name);
        names.remove(name);
        return Response.ok().build();
    }

    @GET
    @Produces({"application/json"})
    public Response getList() {
        final String entity = Jackson.asJsonSilently(names);
        LOG.debug("Returned json: " + entity);
        return Response.ok(entity).build();

    }
}

