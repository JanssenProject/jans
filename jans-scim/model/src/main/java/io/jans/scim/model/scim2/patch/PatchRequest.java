/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.patch;


import static io.jans.scim.model.scim2.Constants.PATCH_REQUEST_SCHEMA_ID;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class used to represent a request for a SCIM PATCH. See section 3.5.2 of RFC 7644.
 */
/*
 * Created by jgomer on 2017-10-28.
 */
public class PatchRequest {

    private List<String> schemas;

    @JsonProperty("Operations")
    private List<PatchOperation> operations;

    /**
     * Constructs an empty PatchRequest initializing its {@link #getSchemas() schemas} field properly.
     */
    public PatchRequest() {
        this.schemas = Collections.singletonList(PATCH_REQUEST_SCHEMA_ID);
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    public List<PatchOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<PatchOperation> operations) {
        this.operations = operations;
    }

}
