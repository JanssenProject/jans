-- Starter DDL for the jansTrustRelationship table (MySQL), used by the gated SQL integration tests
-- (TrustRelationshipRepositorySqlIntegrationTests). HAND-AUTHORED — reconcile with the deployment's
-- jans schema generation before relying on it: column sizes, collation, and the doc_id/dn conventions
-- must match what jans-orm expects for your jans version. jans-orm does NOT create this table.
--
-- Base columns (doc_id primary key, dn, objectClass) are required by jans-orm SQL for every entry.
-- The @JsonObject columns (metadata source, profiles, released attributes, diagnostics) and the
-- multi-valued jansEntityId are stored as JSON. Apply against the schema named by
-- -Dtrust.it.sql.schema before running the integration tests.

CREATE TABLE IF NOT EXISTS jansTrustRelationship (
    doc_id            VARCHAR(64)  NOT NULL,
    objectClass       VARCHAR(48)  DEFAULT NULL,
    dn                VARCHAR(128) DEFAULT NULL,

    inum              VARCHAR(64)  DEFAULT NULL,
    displayName       VARCHAR(128) DEFAULT NULL,
    description       VARCHAR(768) DEFAULT NULL,
    jansTrustNature   VARCHAR(32)  DEFAULT NULL,
    jansTrustStatus   VARCHAR(32)  DEFAULT NULL,
    jansTrustVer      INT          DEFAULT NULL,

    jansEntityId      JSON         DEFAULT NULL,   -- multi-valued (list of URI strings)
    jansMetadataSrc   JSON         DEFAULT NULL,   -- @JsonObject
    jansProfiles      JSON         DEFAULT NULL,   -- @JsonObject
    jansReleasedAttr  JSON         DEFAULT NULL,   -- @JsonObject
    jansActivationDiag JSON        DEFAULT NULL,   -- @JsonObject

    PRIMARY KEY (doc_id)
);

CREATE INDEX jansTrustRelationship_inum      ON jansTrustRelationship (inum);
CREATE INDEX jansTrustRelationship_display   ON jansTrustRelationship (displayName);
