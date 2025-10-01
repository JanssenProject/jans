package io.jans.casa.rest;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a JAX-RS resource method with this annotation to make the endpoint protected. Clients hitting your endpoint
 * must pass a valid OAuth bearer token (with proper scopes) in the request header to have access. Example:
 * <pre>
 *
 * import jakarta.ws.rs.GET;
 * import jakarta.ws.rs.Path;
 *
 * {@literal @}Path("/library")
 * public class LibraryRestService {
 *
 *    {@literal @}GET
 *    {@literal @}Path("/books")
 *    {@literal @}ProtectedApi(scopes = {"read"})
 *    public String getBooks() {
 *        return ...
 *    }
 *
 * }
 * </pre>
 *
 * If all methods of your class are protected using the same scopes, apply the annotation at the class level instead
 *
 * @author jgomer
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ProtectedApi {
	String[] scopes() default {};
}
