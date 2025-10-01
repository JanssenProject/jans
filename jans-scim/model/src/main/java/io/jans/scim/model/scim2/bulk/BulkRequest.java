/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2.bulk;

import io.jans.scim.model.scim2.Constants;

import java.util.Collections;

/**
 * Encapsulates the components of a SCIM BulkRequest (see section 3.7 of RFC 7644).
 * @author Rahat Ali Date: 05.08.2015
 */
/*
 * Updated by jgomer on 2017-11-21.
 */
public class BulkRequest extends BulkBase {

    private Integer failOnErrors;

    /**
     * Creates an empty BulkRequest (initializing its {@link #getSchemas() schemas} properly).
     */
    public BulkRequest(){
        setSchemas(Collections.singletonList(Constants.BULK_REQUEST_SCHEMA_ID));
    }

    public Integer getFailOnErrors() {
        return failOnErrors;
    }

    /**
     * Specifies the number of errors that the service provider will accept before the request processing is terminated
     * and an error response is returned.
     * @param failOnErrors Integer value
     */
    public void setFailOnErrors(Integer failOnErrors) {
        this.failOnErrors = failOnErrors;
    }

}
