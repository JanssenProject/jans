/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.model.scim2;

import static io.jans.scim.model.scim2.Constants.ERROR_RESPONSE_URI;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A class that models data of an error response. See section 3.12 of RFC7644.
 *
 * @author Val Pecaoco
 */
/*
 * Updated by jgomer on 2017-09-14.
 */
public class ErrorResponse implements Serializable {

    private List<String> schemas;

    private String status;
    private ErrorScimType scimType;
    private String detail;

    public ErrorResponse() {
        schemas = Collections.singletonList(ERROR_RESPONSE_URI);
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    /**
     * Retrieves the HTTP status code of the error. E.g. "500"
     * @return A string value
     */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Retrieves the error type. E.g. "invalidFilter"
     * @return A string value
     */
    public String getScimType() {
        return scimType == null ? "" : scimType.getValue();
    }

    public void setScimType(ErrorScimType scimType) {
        this.scimType = scimType;
    }

    /**
     * Retrieves a description of the error
     * @return A string value
     */
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

}
