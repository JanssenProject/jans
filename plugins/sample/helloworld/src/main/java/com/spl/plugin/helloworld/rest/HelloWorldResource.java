package com.spl.plugin.helloworld.rest;

import com.spl.plugin.helloworld.util.Constants;
import com.spl.plugin.helloworld.util.Utils;
import io.jans.as.common.model.registration.Client;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;



@Path("hello")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {    
    
    @Inject
    Logger log;
    
    @Inject
    Utils utils;
        
    @GET
    public Response sayHelloWorld() {
        log.error(" HelloWorldResource::sayHelloWorld() - utils = "+utils+"\n\n");
        String issuer = utils.getIssuer();
        log.error(" HelloWorldResource::sayHelloWorld() - issuer = "+issuer+"\n\n");
        return Response.ok("Hello World!").build();
    }
    
    @GET
    @Path(Constants.INUM_PATH)
    public Response getClient(@PathParam(Constants.INUM) @NotNull String inum) {
        System.out.println("\n HelloWorldResource::getClient() - log = "+log+" ,inum = "+inum+"\n\n");
        log.error("\n HelloWorldResource::getClient() - log = "+log+" ,inum = "+inum+"\n\n");
        Client client = utils.getClient(inum);
        log.error(" HelloWorldResource::getClient() - client = "+client+"\n\n");
        System.out.println(" HelloWorldResource::getClient() - client = "+client+"\n\n");
        return Response.ok(client).build();
    }
    
    @GET
    @Path(Constants.NAME_PATH)
    public Response sayHelloWithName(@PathParam(Constants.NAME) @NotNull String name) {
        return Response.ok("Hello World!" +name).build();
    }

    

}
