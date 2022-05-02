/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim2.client.rest.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;
import io.jans.scim.model.scim2.util.IntrospectUtil;
import io.jans.scim.model.scim2.util.ScimResourceUtil;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import static io.jans.scim.model.scim2.Constants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;

/**
 * A custom provider for deserialization of {@link io.jans.scim.model.scim2.ListResponse io.jans.scim.model.scim2.ListResponse}
 * objects.
 * This allows reading (deserializing) subclasses of {@link io.jans.scim.model.scim2.BaseScimResource io.jans.scim.model.scim2.BaseScimResource}
 * correctly.
 * <p>Developers do not need to manipulate this class for their SCIM applications.</p>
 */
/*
 * A standard way to solve this problem is using polymorphic type handling in jackson but it
 * pollutes resource classes and also might introduce unrecognized attributes in responses that are not part of schema spec
 * Created by jgomer on 2017-10-20.
 */
@Provider
@Consumes({MEDIA_TYPE_SCIM_JSON, MediaType.APPLICATION_JSON})
public class ListResponseProvider implements MessageBodyReader<ListResponse> {

    private Logger logger = LogManager.getLogger(getClass());

    private ObjectMapper mapper=new ObjectMapper();

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    	//logger.info("ListResponseProvider in use.");
        return type.equals(ListResponse.class);
    }

    public ListResponse readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
                                 MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {

        InputStreamReader isr = new InputStreamReader(entityStream, Charset.forName("UTF-8"));
        List<BaseScimResource> resources=null;

        //we will get a LinkedHashMap here...
        Map<String, Object> map=mapper.readValue(isr, new TypeReference<Map<String, Object>>(){});
        //"remove" what came originally
        Object branch=map.remove("Resources");

        if (branch!=null) {

            resources=new ArrayList<>();
            //Here we assume everything is coming from the server correctly (that is, following the spec) and conversions succeed
            for (Object resource : (Collection) branch){
                Map<String, Object> resourceAsMap=IntrospectUtil.strObjMap(resource);
                List<String> schemas=(List<String>) resourceAsMap.get("schemas");

                //Guess the real class of the resource by inspecting the schemas in it
                for (String schema : schemas) {
                    for (Class<? extends BaseScimResource> cls : IntrospectUtil.allAttrs.keySet()) {
                        if (ScimResourceUtil.getSchemaAnnotation(cls).id().equals(schema)) {
                            //Create the object with the proper class
                            resources.add(mapper.convertValue(resource, cls));
                            logger.trace("Found resource of class {} in ListResponse", cls.getSimpleName());
                            break;
                        }
                    }
                }
            }
        }

        ListResponse response=mapper.convertValue(map, ListResponse.class);
        response.setResources(resources);

        return response;

    }

}
