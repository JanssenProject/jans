package io.jans.as.server.service.cdi;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

@Alternative
@Priority(100)
public class HttpServletRequestProducer {
    
    @Produces
    @RequestScoped
    @Named
    @Alternative
    public HttpServletRequest produceWebRequest(
            @Any Instance<HttpServletRequest> instances) {
        
        // Get the built-in Weld HttpServletRequest (not the gRPC one)
        return instances.stream()
            .filter(req -> !req.getClass().getName()
                .contains("grpc"))
            .findFirst()
            .orElseThrow();
    }
}