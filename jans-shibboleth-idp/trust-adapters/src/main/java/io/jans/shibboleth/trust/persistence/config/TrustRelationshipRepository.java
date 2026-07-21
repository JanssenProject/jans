package io.jans.shibboleth.trust.persistence.config;

import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.shared.Result;

/**
 * Storage of the {@link TrustRelationship} aggregate (see {@code docs/trustrelationship_persistence_design.md}).
 *
 * <p>Whole-object operations map to/from the domain aggregate; {@link #list} returns view summaries via a
 * query projection that never materializes the aggregate (TP10/TP11). Every operation reports failure
 * through {@link Result}/{@code DomainError} rather than throwing.
 */
public interface TrustRelationshipRepository {

    /**
     * Inserts a new trust relationship (assigning its id, TP5/D10) or updates an existing one. Returns the
     * stored aggregate carrying its assigned id.
     */
    Result<TrustRelationship> save(TrustRelationship trustRelationship);

    /** Loads the full aggregate by id (rehydrated + validated); a missing id is a failure, not {@code null}. */
    Result<TrustRelationship> findById(Id id);

    /** Lists trust relationships as view summaries (read models), filtered/paged/sorted per the query (D14). */
    Result<TrustRelationshipSummaryPage> list(TrustRelationshipQuery query);

    /** Hard-deletes the trust relationship with the given id (D11). */
    Result<Void> delete(Id id);
}
