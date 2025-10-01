package io.jans.plugin.demo.rest;


import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;


public class DemoBaseResource {

    public static <T> void checkResourceNotNull(T resource, String objectName) {
        if (resource == null) {
            throw new NotFoundException(getNotFoundError(objectName));
        }
    }

    public static <T> void checkNotNull(String attribute, String attributeName) {
        if (attribute == null) {
            throw new BadRequestException(getMissingAttributeError(attributeName));
        }
    }

    public static <T> void checkNotEmpty(List<T> list, String attributeName) {
        if (list == null || list.isEmpty()) {
            throw new BadRequestException(getMissingAttributeError(attributeName));
        }
    }

    /**
     * @param attributeName
     * @return
     */
    protected static Response getMissingAttributeError(String attributeName) {
        String error = "The attribute " + attributeName + " is required for this operation";
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    protected static Response getNotFoundError(String objectName) {
        String error = "The requested " + objectName + " doesn't exist";
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }

    protected static Response getNotAcceptableException(String msg) {
        String error = msg;
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(error).build();
    }

}
