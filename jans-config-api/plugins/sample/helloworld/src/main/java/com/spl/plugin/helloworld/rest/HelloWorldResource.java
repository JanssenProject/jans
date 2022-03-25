package com.spl.plugin.helloworld.rest;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;

import com.spl.plugin.helloworld.util.Constants;

@Path("hello")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {    
    
    @Inject
    Logger log;
        
    @GET
    public Response sayHelloWorld() {
        return Response.ok("Hello World!").build();
    }    
    
    @GET
    @Path(Constants.NAME_PATH)
    public Response sayHelloWithName(@PathParam(Constants.NAME) @NotNull String name) {
        return Response.ok("Hello World!"+name).build();
    }

    

}
