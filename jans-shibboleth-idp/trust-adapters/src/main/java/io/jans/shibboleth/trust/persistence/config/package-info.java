/**
 * Outbound persistence adapter for the {@code config} bounded context, backed by {@code jans-orm}.
 *
 * <p>Two paths (see {@code docs/trustrelationship_persistence_design.md}):
 * <ul>
 *   <li><b>Whole-object</b> ({@code save}/{@code findById}) maps between the storage entry and the
 *       domain aggregate {@code TrustRelationship}, rehydrating a validated aggregate on read (TP1).</li>
 *   <li><b>Query</b> ({@code list}) is a projection that builds the view summary DTO directly from a
 *       reduced-attribute entry, bypassing the domain — a query projection, not a domain mapper
 *       (TP10/TP11).</li>
 * </ul>
 */
package io.jans.shibboleth.trust.persistence.config;
