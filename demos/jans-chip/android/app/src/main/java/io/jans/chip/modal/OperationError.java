package io.jans.chip.modal;

import com.google.common.base.Preconditions;

public class OperationError {
    private String title;
    private String message;

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public OperationError(Builder builder) {
        this.title = builder.title;
        this.message = builder.message;
    }
    public static class Builder {
        private String title;
        private String message;
        public Builder() {
        }
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        public OperationError build() {
            return new OperationError(this);
        }
    }
}
