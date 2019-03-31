package org.gluu.oxd.common.response;

/**
 * @author Yuriy Zabrovarnyy
 */
public class POJOResponse implements IOpResponse {

    private final Object node;

    public POJOResponse(Object node) {
        this.node = node;
    }

    public Object getNode() {
        return node;
    }
}
