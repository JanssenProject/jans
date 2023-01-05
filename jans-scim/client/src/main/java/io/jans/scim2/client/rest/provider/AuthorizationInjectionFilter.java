/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest.provider;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jans.scim2.client.ClientMap;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.ext.Provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    
    private ClientMap clientMap = ClientMap.instance();

    public void filter(ClientRequestContext context) {

        Client client = context.getClient();
        MultivaluedMap<String, Object> headers = context.getHeaders();
        String authzHeader = clientMap.getValue(client);

        if (StringUtils.isNotEmpty(authzHeader)) {   //resteasy client is tied to an authz header
            headers.putSingle("Authorization", authzHeader);
        }

        //Inject app-specific custom headers
        MultivaluedMap<String, String> map = Optional.ofNullable(clientMap.getCustomHeaders(client))
        	.orElse(new MultivaluedHashMap<>());
        for (String key : map.keySet()) {
        	//strangely, headers is <String, Object> but it should be <String, String>
        	List<Object> list = new ArrayList<>();
        	map.get(key).forEach(list::add);
        	headers.put(key, list);
        }

        //Inject jvm-level headers
        Optional.ofNullable(System.getProperty("scim.extraHeaders"))
                .map(str -> Arrays.asList(str.split(",\\s*"))).orElse(Collections.emptyList())
                .forEach(prop ->
                        Optional.ofNullable(System.getProperty("scim.header." + prop))
                                .ifPresent(value -> headers.putSingle(prop,  value))
                );

    }

}
