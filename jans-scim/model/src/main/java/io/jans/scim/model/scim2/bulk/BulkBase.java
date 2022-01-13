/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.bulk;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A class that abstracts the common properties of a {@link BulkRequest BulkRequest} or {@link BulkResponse BulkResponse}.
 * See section 3.7 of RFC 7644.
 */
/*
 * Created by jgomer on 2017-11-21.
 */
public class BulkBase {

    private List<String> schemas;

    @JsonProperty("Operations")
    private List<BulkOperation> operations;

    BulkBase(){}

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    public List<BulkOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<BulkOperation> operations) {
        this.operations = operations;
    }

    //Writing JSON without the capital letter "O" is a common error. We should keep this method
    @JsonProperty("operations")
    public void setOperations2(List<BulkOperation> misSpelledOperations) {
        setOperations(misSpelledOperations);
    }

}
