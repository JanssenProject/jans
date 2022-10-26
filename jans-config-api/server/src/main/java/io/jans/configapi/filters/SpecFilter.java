/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.filters;

import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.model.ApiDescription;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

public class SpecFilter extends AbstractSpecFilter {

    @Override
    public Optional<Operation> filterOperation(Operation operation, ApiDescription api,
            Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
        try {

            if (operation != null) {

                // request example
                if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                    for (MediaType mediaType : operation.getRequestBody().getContent().values()) {
                        if (mediaType == null || mediaType.getExamples() == null
                                || mediaType.getExamples().values() == null)
                            continue;
                        for (Example example : mediaType.getExamples().values()) {
                            if (example == null) {
                                continue;
                            }

                            if (example.getValue() != null && example.getValue().toString().endsWith(".json")) {
                                example.setValue(getExample(example.getValue().toString()));
                            }

                        }
                    }
                }

                // response example
                if (operation.getResponses() != null && !operation.getResponses().isEmpty()) {

                    for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {

                        ApiResponse apiResponse = responseEntry.getValue();
                        if (apiResponse == null) {
                            continue;
                        }

                        if (apiResponse.getContent() != null) {
                            for (Map.Entry<String, MediaType> mediaEntry : apiResponse.getContent().entrySet()) {

                                if (mediaEntry.getValue() != null && mediaEntry.getValue().getExamples() != null) {
                                    for (Example example : mediaEntry.getValue().getExamples().values()) {
                                        if (example == null) {
                                            continue;
                                        }

                                        if (example.getValue() != null
                                                && example.getValue().toString().endsWith(".json")) {
                                            example.setValue(getExample(example.getValue().toString()));
                                        }

                                    }
                                }
                            }
                        }

                    }

                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return Optional.of(operation);
    }
    
    private String setRequestExample(final String fileName) throws IOException {
        //???-TO_DO??
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        return getExampleContent(inputStream);

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