-- Trust activation bounded context: work items, their leases, workers, and current-episode pointers.
-- The lease lock rests entirely on doc_id being the primary key: jans-orm derives doc_id from the first RDN
-- value (the inum), and a lease's inum is a deterministic hash of (workItemId, generation) — so two workers
-- racing for the same generation compute the same doc_id and collide on this PRIMARY KEY. The PK *is* the lock.

SET search_path TO public;

-- public."jansTrustActivationWorkItem" definition

CREATE TABLE "jansTrustActivationWorkItem" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"jansWorkItemType" varchar(64) NULL,
	"jansTrId" varchar(64) NULL,
	"jansWorkItemStatus" varchar(64) NULL,
	"jansCreatedAt" timestamp NULL,
	"jansLastTransitionAt" timestamp NULL,
	CONSTRAINT "jansTrustActivationWorkItem_pkey" PRIMARY KEY (doc_id)
);
CREATE INDEX "jansTrustActivationWorkItem_type_status" ON "jansTrustActivationWorkItem" ("jansWorkItemType", "jansWorkItemStatus");

-- public."jansTrustActivationLease" definition

CREATE TABLE "jansTrustActivationLease" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"jansWorkItemRef" varchar(64) NULL,
	"jansLeaseGen" int4 NULL,
	"jansLeaseWorker" varchar(128) NULL,
	"jansLeaseGrantedAt" timestamp NULL,
	"jansLeaseExpiresAt" timestamp NULL,
	CONSTRAINT "jansTrustActivationLease_pkey" PRIMARY KEY (doc_id)
);
CREATE INDEX "jansTrustActivationLease_ref" ON "jansTrustActivationLease" ("jansWorkItemRef");

-- public."jansTrustActivationWorker" definition — inum is a deterministic name-based UUID of jansWorkerOrigin

CREATE TABLE "jansTrustActivationWorker" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"jansWorkerOrigin" varchar(128) NULL,
	"jansRegisteredAt" timestamp NULL,
	"jansLastHeartbeatAt" timestamp NULL,
	CONSTRAINT "jansTrustActivationWorker_pkey" PRIMARY KEY (doc_id)
);

-- public."jansTrustActivationEpisode" definition — one current-episode pointer per trust relationship (keyed by trId)

CREATE TABLE "jansTrustActivationEpisode" (
	doc_id varchar(64) NOT NULL,
	"objectClass" varchar(48) NULL,
	dn varchar(128) NULL,
	inum varchar(64) NULL,
	"jansWorkItemRef" varchar(64) NULL,
	CONSTRAINT "jansTrustActivationEpisode_pkey" PRIMARY KEY (doc_id)
);
