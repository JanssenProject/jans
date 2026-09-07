package io.jans.ca.plugin.adminui.model.adminui;

/**
 * Jackson {@code @JsonView} marker interfaces used to control which
 * {@link AdminUIPolicyStore} fields may be supplied by clients on different
 * REST operations.
 *
 * <ul>
 *     <li>{@link Create} - fields writable on POST (create).</li>
 *     <li>{@link Edit} - fields writable on PUT (edit). Read-only fields
 *         (e.g. {@code inum}, {@code dn}, {@code creationDate},
 *         {@code jansUsrDN}, {@code policyStore}) are intentionally excluded
 *         from this view so they cannot be modified once created.</li>
 * </ul>
 */
public final class PolicyStoreViews {

    private PolicyStoreViews() {
    }

    /** Fields accepted when creating a policy store (POST). */
    public interface Create {
    }

    /** Fields accepted when editing an existing policy store (PUT). */
    public interface Edit {
    }
}
