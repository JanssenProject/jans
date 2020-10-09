package io.jans.scim2.client.rest.provider;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jans.scim2.client.ClientMap;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * A Client-side filter employed to "inject" headers to the outgoing request. This filter applies only for
 * requests issued by concrete instances of {@link io.jans.scim2.client.AbstractScimClient AbstractScimClient} class.
 * <p>Developers do not need to manipulate this class for their SCIM applications.</p>
 */
/*
 * Created by jgomer on 2017-11-25.
 */
@Provider
public class AuthorizationInjectionFilter implements ClientRequestFilter {

    private Logger logger = LogManager.getLogger(getClass());

    public void filter(ClientRequestContext context) {

        MultivaluedMap<String, Object> headers = context.getHeaders();
        String authzHeader = ClientMap.getValue(context.getClient());

        if (StringUtils.isNotEmpty(authzHeader)) {   //resteasy client is tied to an authz header
            headers.putSingle("Authorization", authzHeader);
        }
        //Inject custom headers
        Optional.ofNullable(System.getProperty("scim.extraHeaders"))
                .map(str -> Arrays.asList(str.split(",\\s*"))).orElse(Collections.emptyList())
                .forEach(prop ->
                        Optional.ofNullable(System.getProperty("scim.header." + prop))
                                .ifPresent(value -> headers.putSingle(prop,  value))
                );

    }

}
