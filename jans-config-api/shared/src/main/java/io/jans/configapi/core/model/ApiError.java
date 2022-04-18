/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.core.model;

import java.io.Serializable;

/**
 * @author Mougang T.Gasmyr
 *
 */
public class ApiError implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -3836623519481821884L;
    private String code;
    private String message;
    private String description;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ApiError [code=" + code + ", message=" + message + ", description=" + description + "]";
    }

    public static class ErrorBuilder {
        private String code;
        private String message;
        private String description;

        public ErrorBuilder withCode(String code) {
            this.code = code;
            return this;
        }

        public ErrorBuilder withMessage(String messge) {
            this.message = messge;
            return this;
        }

        public ErrorBuilder andDescription(String description) {
            this.description = description;
            return this;
        }

        public ApiError build() {
            ApiError error = new ApiError();
            error.code = this.code;
            error.message = this.message;
            error.description = this.description;
            return error;
        }

    }

}
