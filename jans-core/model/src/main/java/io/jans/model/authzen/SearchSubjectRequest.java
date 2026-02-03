package io.jans.model.authzen;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * AuthZEN Search Subject Request.
 * Used for POST /access/v1/search/subject endpoint.
 * Discovers subjects authorized for a given action on a resource.
 *
 * @author Yuriy Z
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchSubjectRequest implements Serializable {

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

    public SearchSubjectRequest() {
    }

    public Subject getSubject() {
        return subject;
    }

    public SearchSubjectRequest setSubject(Subject subject) {
        this.subject = subject;
        return this;
    }

    public Resource getResource() {
        return resource;
    }

    public SearchSubjectRequest setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public SearchSubjectRequest setAction(Action action) {
        this.action = action;
        return this;
    }

    public Context getContext() {
        return context;
    }

    public SearchSubjectRequest setContext(Context context) {
        this.context = context;
        return this;
    }

    public PageRequest getPage() {
        return page;
    }

    public SearchSubjectRequest setPage(PageRequest page) {
        this.page = page;
        return this;
    }

    @Override
    public String toString() {
        return "SearchSubjectRequest{" +
                "subject=" + subject +
                ", resource=" + resource +
                ", action=" + action +
                ", context=" + context +
                ", page=" + page +
                '}';
    }
}
