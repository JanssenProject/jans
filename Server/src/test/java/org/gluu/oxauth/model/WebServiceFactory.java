package org.gluu.oxauth.model;

import org.gluu.oxauth.register.ws.rs.RegisterRestWebService;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.core.MediaType;

/**
 * @author Yuriy Zabrovarnyy
 */
public class WebServiceFactory {

    public static final String RESTV_1 = "restv1";

    private WebServiceFactory() {
    }

    public static WebServiceFactory instance() {
        return new WebServiceFactory();
    }

    public RegisterRestWebService createRegisterWs(String url) {
        return createRegisterWs(RegisterRestWebService.class, url, MediaType.APPLICATION_JSON_TYPE);
    }

    public <T> T createRegisterWs(Class<T> clazz, String url, MediaType consumeMediaType) {
        final ResteasyWebTarget target = (ResteasyWebTarget) ResteasyClientBuilder.newClient().target(url + RESTV_1);
        return target.proxyBuilder(clazz).defaultConsumes(consumeMediaType).build();
    }
}
