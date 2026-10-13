-- File-staging metadata table (the file bytes live in the jans document store, not here).
--
-- Storage-visible names the jans setup must provision for the file-staging service:
--   object class / SQL table : jansStagedFile
--   branch                   : ou=stagedFiles,o=jans
-- The DN is the primary key; jans-orm derives doc_id from the first RDN value (the inum), and a staged
-- file's inum IS its opaque token. Timestamps MUST be a real timestamp type (never varchar): jans-orm owns
-- the date codec, and a varchar column silently reformats Instant values on read.

SET search_path TO public;

-- public."jansStagedFile" definition

CREATE TABLE "jansStagedFile" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"jansFileName" varchar(255) NULL,
	"jansContentHash" varchar(128) NULL,
	"jansContentSize" bigint NULL,
	"jansContentType" varchar(128) NULL,
	"jansStagedFileStatus" varchar(16) NULL,
	"jansStagedAt" timestamp NULL,
	"jansExpiresAt" timestamp NULL,
	"jansHandle" varchar(512) NULL,
	CONSTRAINT "jansStagedFile_pkey" PRIMARY KEY (doc_id)
);

-- Supports findExpiredUnclaimed: filter STAGED rows, then apply the expiry cutoff.
CREATE INDEX "jansStagedFile_status" ON "jansStagedFile" ("jansStagedFileStatus");
