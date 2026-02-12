package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AuthZEN Search Resource Request.
 * Used for POST /access/v1/search/resource endpoint.
 * Discovers resources a subject is authorized to access for a given action.
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResourceRequest {

    @JsonProperty("subject")
    private Subject subject;

    @JsonProperty("resource")
    private Resource resource;

    @JsonProperty("action")
    private Action action;

    @JsonProperty("context")
    private Context context;

    @JsonProperty("page")
    private PageRequest page;

    public SearchResourceRequest() {
    }

    public Subject getSubject() {
        return subject;
    }

    public SearchResourceRequest setSubject(Subject subject) {
        this.subject = subject;
        return this;
    }

    public Resource getResource() {
        return resource;
    }

    public SearchResourceRequest setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public SearchResourceRequest setAction(Action action) {
        this.action = action;
        return this;
    }

    public Context getContext() {
        return context;
    }

    public SearchResourceRequest setContext(Context context) {
        this.context = context;
        return this;
    }

    public PageRequest getPage() {
        return page;
    }

    public SearchResourceRequest setPage(PageRequest page) {
        this.page = page;
        return this;
    }

    @Override
    public String toString() {
        return "SearchResourceRequest{" +
                "subject=" + subject +
                ", resource=" + resource +
                ", action=" + action +
                ", context=" + context +
                ", page=" + page +
                '}';
    }
}
