package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AuthZEN Search Action Request.
 * Used for POST /access/v1/search/action endpoint.
 * Discovers actions a subject is authorized to perform on a resource.
 * Note: Per spec, the action field is omitted for action search.
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchActionRequest {

    @JsonProperty("subject")
    private Subject subject;

    @JsonProperty("resource")
    private Resource resource;

    @JsonProperty("context")
    private Context context;

    @JsonProperty("page")
    private PageRequest page;

    public SearchActionRequest() {
        // empty
    }

    public Subject getSubject() {
        return subject;
    }

    public SearchActionRequest setSubject(Subject subject) {
        this.subject = subject;
        return this;
    }

    public Resource getResource() {
        return resource;
    }

    public SearchActionRequest setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    public Context getContext() {
        return context;
    }

    public SearchActionRequest setContext(Context context) {
        this.context = context;
        return this;
    }

    public PageRequest getPage() {
        return page;
    }

    public SearchActionRequest setPage(PageRequest page) {
        this.page = page;
        return this;
    }

    @Override
    public String toString() {
        return "SearchActionRequest{" +
                "subject=" + subject +
                ", resource=" + resource +
                ", context=" + context +
                ", page=" + page +
                '}';
    }
}
