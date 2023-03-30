/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;

import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;

import java.io.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class SpecFilter extends AbstractSpecFilter {

    @Override
    public Optional<Operation> filterOperation(Operation operation, ApiDescription api,
            Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        try {

            if (operation != null) {

                setRequestExample(operation);
                setResponseExample(operation);
            
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
        return Optional.of(operation);
    }
    
    @Override
    public Optional filterSchema(Schema schema, Map params, Map cookies, Map headers){
        Schema newUserSchema = new Schema<Map<String, Object>>()
                .addProperties("name",new Schema().example("John123"))
                .addProperties("password",new Schema().example("P4SSW0RD"))
                .addProperties("image",new Schema().example("https://robohash.org/John123.png"));

        
        Schema clientAuthSchema1 =  new Schema()
                .description("clientAuthSchema1")
                .addProperties("children", new MapSchema()
                        .additionalProperties(new Schema().$ref("#/components/schemas/Scope")));
        
        Schema clientAuthMapSchema =  new Schema()
                .description("clientAuthMapSchema")
                .addProperties("children", new MapSchema()
                .additionalProperties(new Schema().$ref("#/components/schemas/Scope")));

        Schema clientAuthMapArraySchema =  new Schema()
                .description("clientAuthMapArraySchema")
                .addProperties("children", new MapSchema()
                .additionalProperties(new ArraySchema().$ref("#/components/schemas/Scope")));
        
        schema.addProperty("newUserSchema", newUserSchema);
        schema.addProperty("clientAuthSchema1", clientAuthSchema1);
        schema.addProperty("clientAuthMapSchema", clientAuthMapSchema);
        schema.addProperty("clientAuthMapArraySchema", clientAuthMapArraySchema);
        
        return Optional.of(schema);
    }

    private void setRequestExample(Operation operation) {
        // request example
        if (operation != null && operation.getRequestBody() != null
                && operation.getRequestBody().getContent() != null) {
            try {
                for (MediaType mediaType : operation.getRequestBody().getContent().values()) {
                    if (mediaType == null || mediaType.getExamples() == null
                            || mediaType.getExamples().values() == null)
                        continue;
                    for (Example example : mediaType.getExamples().values()) {
                        setExample(example);
                    }
                }
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }

    }

    private void setResponseExample(Operation operation) {
        // response example
        if (operation != null && operation.getResponses() != null && !operation.getResponses().isEmpty()) {
            try {
                for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {

                    ApiResponse apiResponse = responseEntry.getValue();
                    if (apiResponse == null) {
                        continue;
                    }

                    if (apiResponse.getContent() != null) {
                        for (Map.Entry<String, MediaType> mediaEntry : apiResponse.getContent().entrySet()) {

                            if (mediaEntry.getValue() != null && mediaEntry.getValue().getExamples() != null) {
                                for (Example example : mediaEntry.getValue().getExamples().values()) {
                                    setExample(example);
                                }
                            }
                        }
                    }

                }

            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    private void setExample(Example example) throws IOException {
        if (example == null) {
            return;
        }

        if (example.getValue() != null && example.getValue().toString().endsWith(".json")) {
            example.setValue(getExample(example.getValue().toString()));
        }

    }

    private String getExample(final String fileName) throws IOException {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        return getExampleContent(inputStream);

    }

    private String getExampleContent(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStreamReader isr = new InputStreamReader(is); BufferedReader br = new BufferedReader(isr);) {
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            is.close();
        }

        return stringBuilder.toString();
    }
}