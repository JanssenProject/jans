package io.jans.as.common.model.session;

/**
 * @author Yuriy Z
 */
public class SessionIdPredefinedAttributes {
    // index in status list
    private Integer index;

    public Integer getIndex() {
        return index;
    }

    public SessionIdPredefinedAttributes setIndex(Integer index) {
        this.index = index;
        return this;
    }

    @Override
    public String toString() {
        return "SessionIdPredefinedAttributes{" +
                "index=" + index +
                '}';
    }
}
