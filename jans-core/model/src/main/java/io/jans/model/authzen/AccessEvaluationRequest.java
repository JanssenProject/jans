package io.jans.model.authzen;

import java.io.Serializable;

/**
 * @author Yuriy Z
 */
public class AccessEvaluationRequest implements Serializable {

    private Subject subject;
    private Resource resource;
    private Action action;
    private Context context;

    public AccessEvaluationRequest() {
    }

    public AccessEvaluationRequest(Subject subject, Resource resource, Action action, Context context) {
        this.subject = subject;
        this.resource = resource;
        this.action = action;
        this.context = context;
    }

    public Subject getSubject() {
        return subject;
    }

    public Resource getResource() {
        return resource;
    }

    public Action getAction() {
        return action;
    }

    public Context getContext() {
        return context;
    }

    public AccessEvaluationRequest setSubject(Subject subject) {
        this.subject = subject;
        return this;
    }

    public AccessEvaluationRequest setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    public AccessEvaluationRequest setAction(Action action) {
        this.action = action;
        return this;
    }

    public AccessEvaluationRequest setContext(Context context) {
        this.context = context;
        return this;
    }

    @Override
    public String toString() {
        return "AccessEvaluationRequest{" +
                "subject=" + subject +
                ", resource=" + resource +
                ", action=" + action +
                ", context=" + context +
                '}';
    }
}
