/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.service.scim2.serialization;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.jans.scim.model.scim2.BaseScimResource;
import io.jans.scim.model.scim2.ListResponse;

/**
 * Created by jgomer on 2017-10-01.
 */
public class ListResponseJsonSerializer extends JsonSerializer<ListResponse> {

    private ScimResourceSerializer resourceSerializer;
    private ObjectMapper mapper = new ObjectMapper();

    private String attributes;
    private String excludeAttributes;
    private boolean skipResults;

    private List<JsonNode> jsonResources;

    //why not to inject the resource serializer instead of passing it as parameter? weld simply does not like it!
    public ListResponseJsonSerializer(ScimResourceSerializer serializer) {
        resourceSerializer = serializer;
    }

    public ListResponseJsonSerializer(ScimResourceSerializer serializer, String attributes, String excludeAttributes, boolean skipResults) {
        resourceSerializer = serializer;
        this.attributes = attributes;
        this.excludeAttributes = excludeAttributes;
        this.skipResults = skipResults;
    }

    public void setJsonResources(List<JsonNode> resources) {
        this.jsonResources = resources;
    }

    @Override
    public void serialize(ListResponse listResponse, JsonGenerator jGen, SerializerProvider provider) throws IOException {

        try {
            jGen.writeStartObject();

            jGen.writeArrayFieldStart("schemas");
            for (String schema : listResponse.getSchemas())
                jGen.writeString(schema);
            jGen.writeEndArray();

            jGen.writeNumberField("totalResults", listResponse.getTotalResults());

            if (!skipResults) {
                if (listResponse.getItemsPerPage() > 0) {
                    //these two bits are "REQUIRED when partial results are returned due to pagination." (section 3.4.2 RFC 7644)
                    jGen.writeNumberField("startIndex", listResponse.getStartIndex());
                    jGen.writeNumberField("itemsPerPage", listResponse.getItemsPerPage());
                }

                //Section 3.4.2 RFC 7644: Resources [...] REQUIRED if "totalResults" is non-zero
                if (listResponse.getTotalResults() > 0) {
                    jGen.writeArrayFieldStart("Resources");

                    if (listResponse.getResources().size() > 0) {
                        for (BaseScimResource resource : listResponse.getResources()) {
                            JsonNode jsonResource = mapper.readTree(resourceSerializer.serialize(resource, attributes, excludeAttributes));
                            jGen.writeTree(jsonResource);
                        }
                    } else if (jsonResources != null) {
                        for (JsonNode node : jsonResources) {
                            jGen.writeTree(node);
                        }
                    }
                    jGen.writeEndArray();
                }
            }

            jGen.writeEndObject();
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

}
