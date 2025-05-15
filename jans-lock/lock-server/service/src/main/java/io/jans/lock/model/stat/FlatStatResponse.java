package io.jans.lock.model.stat;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.resteasy.annotations.providers.jaxb.IgnoreMediaTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Movchan Date: 12/02/2024
 */
@IgnoreMediaTypes("application/*+json")
public class FlatStatResponse {
    @JsonProperty(value = "response") // month to stat item
    private List<StatResponseItem> response = new ArrayList<>();

    public FlatStatResponse() {
    }

    public FlatStatResponse(List<StatResponseItem> response) {
        this.response = response;
    }

    public List<StatResponseItem> getResponse() {
        if (response == null) response = new ArrayList<>();
        return response;
    }

    public void setResponse(List<StatResponseItem> response) {
        this.response = response;
    }

    @Override
	public String toString() {
		return "FlatStatResponse [response=" + response + "]";
	}
}
