package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * AuthZEN Search Response.
 * Common response for all search endpoints (subject, resource, action).
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse<T> implements Serializable {

    @JsonProperty("page")
    private PageResponse page;

    @JsonProperty("context")
    private Context context;

    @JsonProperty("results")
    private List<T> results;

    public SearchResponse() {
    }

    public SearchResponse(List<T> results, PageResponse page) {
        this.results = results;
        this.page = page;
    }

    public PageResponse getPage() {
        return page;
    }

    public SearchResponse<T> setPage(PageResponse page) {
        this.page = page;
        return this;
    }

    public Context getContext() {
        return context;
    }

    public SearchResponse<T> setContext(Context context) {
        this.context = context;
        return this;
    }

    public List<T> getResults() {
        return results;
    }

    public SearchResponse<T> setResults(List<T> results) {
        this.results = results;
        return this;
    }

    @Override
    public String toString() {
        return "SearchResponse{" +
                "page=" + page +
                ", context=" + context +
                ", results=" + results +
                '}';
    }
}
