-- Optional: create a custom schema namespace
CREATE SCHEMA IF NOT EXISTS public;

SET search_path TO public;

-- Drop table

-- DROP TABLE "jansTrustRelationship";

CREATE TABLE "jansTrustRelationship" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"displayName" varchar(128) NULL,
	description varchar(768) NULL,
	"jansTrustNature" varchar(64) NULL,
	"jansTrustStatus" varchar(64) NULL,
	"jansTrustVer" int4 NULL,
	"jansEntityId" jsonb NULL,
	"jansMetadataSrc" jsonb NULL,
	"jansProfiles" jsonb NULL,
	"jansReleasedAttr" jsonb NULL,
	"jansActivationDiag" jsonb NULL,
	CONSTRAINT "jansTrustRelationship_pkey" PRIMARY KEY (doc_id)
);