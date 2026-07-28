-- Activation bounded context: work items, their leases, and workers.
-- The lease lock rests entirely on doc_id being the primary key: jans-orm derives doc_id from the first RDN
-- value (the inum), and a lease's inum is a deterministic hash of (workItemId, generation) — so two workers
-- racing for the same generation compute the same doc_id and collide on this PRIMARY KEY. The PK *is* the lock.

SET search_path TO public;

-- public."jansWorkItem" definition

CREATE TABLE "jansWorkItem" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"jansWorkItemType" varchar(64) NULL,
	"jansTrId" varchar(64) NULL,
	"jansWorkItemStatus" varchar(64) NULL,
	"jansCreatedAt" varchar(64) NULL,
	"jansLastTransitionAt" varchar(64) NULL,
	CONSTRAINT "jansWorkItem_pkey" PRIMARY KEY (doc_id)
);
CREATE INDEX "jansWorkItem_type_status" ON "jansWorkItem" ("jansWorkItemType", "jansWorkItemStatus");

-- public."jansWorkItemLease" definition

CREATE TABLE "jansWorkItemLease" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"jansWorkItemRef" varchar(64) NULL,
	"jansLeaseGen" int4 NULL,
	"jansLeaseWorker" varchar(128) NULL,
	"jansLeaseGrantedAt" varchar(64) NULL,
	"jansLeaseExpiresAt" varchar(64) NULL,
	CONSTRAINT "jansWorkItemLease_pkey" PRIMARY KEY (doc_id)
);
CREATE INDEX "jansWorkItemLease_ref" ON "jansWorkItemLease" ("jansWorkItemRef");

-- public."jansActivationWorker" definition

CREATE TABLE "jansActivationWorker" (
	doc_id varchar(128) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(192) NULL,
	inum varchar(128) NULL,
	"jansRegisteredAt" varchar(64) NULL,
	"jansLastHeartbeatAt" varchar(64) NULL,
	CONSTRAINT "jansActivationWorker_pkey" PRIMARY KEY (doc_id)
);
