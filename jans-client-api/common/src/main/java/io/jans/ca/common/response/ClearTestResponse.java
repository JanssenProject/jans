package io.jans.ca.common.response;


import com.fasterxml.jackson.annotation.JsonProperty;

public class ClearTestResponse implements IOpResponse {

    @JsonProperty(value = "result")
    private String result;

    public ClearTestResponse() {
    }

    public ClearTestResponse(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
