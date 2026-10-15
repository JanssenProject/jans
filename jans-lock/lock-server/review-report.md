# Lock Server TRACE code review against the Oct 13 design

Code conforms to the Oct 13 MVP design in every chapter: domain derivation, key scope, JCS, signature and digest scopes, verification block, four flags, equivocation, idempotency, bulk envelope, retrieval ambiguity, receipt hash with millisecond received_at.

Reviewed: `jans-lock/lock-server` main sources (`model`, `service`, `server`), the setup
templates and schema under `jans-linux-setup`, `lock-server.yaml`, and
`docs/janssen-server/lock/trace.md`. Reviewed against:

- `Lock-Server-TRACE-MVP-Design - Oct 13.md` (called "MVP" below) — primary.
- `govops-trace-profile.md` (called "GovOps" below) — secondary.

Status vocabulary:

| Status | Meaning |
|---|---|
| Conforms | Code matches the chapter. |
| Conforms (documented deviation) | Behaviour differs in a way the chapter text does not cover or forbids literally; the deviation is documented in `trace.md` and listed in Task 01 for the design text. |
| Deferred | The MVP design itself defers the item. Not tracked as work. |
| Deployment | Operational requirement outside the code. |
| Not verified | Tests out of scope, or depends on an unavailable document. |

## 1. MVP design, chapter by chapter

| § | Chapter | Status | Notes |
|---|---|---|---|
| 1 | Purpose | Conforms | Attributable, tamper-evident records plus direct correlation; no completeness claims. |
| 2 | MVP outcome | Conforms | Every bullet implemented. |
| 3 | Scope | Conforms | Required list complete. Key registration is admin API only; the chapter allows configuration or API. Deferred list honoured. |
| 4 | Security and responsibility boundaries | Conforms | OAuth scope check, then key lookup, then signature. |
| 4.1 | Evidence-domain isolation | Conforms | Domain derived from the client binding only; body `evidence_domain_id` rejected. Default-domain fallback now grants `defaultAllowedProducerIds` (default empty), never `"*"`. |
| 4.2 | Submitting client and record producer | Conforms | Forwarding allowed only through an explicit binding or the configured default list. |
| 5 | Minimal architecture | Conforms (documented deviation) | Steps 1–9 in order. Step 8 "atomically" is a claim/insert/commit protocol with a repair timer; jans-orm has no transactions. Documented in `trace.md` "Receipt chain". |
| 6 | Producer key registration | Conforms | Admin-only under `trace.admin`; `authorized_event_kinds` enforced before crypto; stable `producer_id`; revocation never rewrites stored verification. `authorized_measurement_points` deferred by the chapter. |
| 7 | Minimal TRACE assertion | Conforms | Wire shape as specified. `cnf` and every Lock-derived member now rejected (`forbidden:<path>`). |
| 7.1 | Required common fields | Conforms (documented deviation) | All rules enforced. Added rule not in the chapter: `signed_at` more than `maxFutureSkewSeconds` (default 300) ahead of Lock's clock is rejected (`signed_at_in_future`). Documented in `trace.md` "Lateness". |
| 7.1 | Canonical propagation wire profile | Deployment | Producer and ingress concern; Lock consumes signed values only. |
| 7.2 | MVP event catalog | Conforms | Three kinds; singular/plural facts; `resource_id`, record-level `outcome`, `capability_ids` forbidden; decision outcome `ALLOW`/`DENY`; invocation outcome `SUCCESS`/`FAILURE`; `trace.policy` required on decisions and optional-but-validated on invocations; optional digests type-checked. |
| 7.2 | What each record establishes | Conforms | No promotion of a record into a stronger claim. |
| 7.3 | Token references | Conforms | Issuer + type + exactly one of `jti`/`fingerprint`; raw token material rejected. |
| 8 | Producer chain | Conforms | Multi-chain per instance; genesis rule; both registration modes; flag clearing rules; equivocation stores both. `chain_link` is shape-checked and genesis-only; link validation deferred by the chapter. |
| 9 | Lock receipt chain | Conforms (documented deviation) | Per-domain monotonic sequence, millisecond `received_at`, `prev_receipt_hash`. Hash input is the chapter's seven members plus `receipt_profile` (`tag:jans.io,2026:lock-trace-receipt-v1`), which satisfies "at least". `VOID` entries occupy positions permanently; a verifier must accept them. Documented in `trace.md`. |
| 10 | Stored envelope | Conforms | Exact received text stored; `content_digest` over JCS of the full assertion; six verification members fixed at admission. Retrieval now returns `assertion_raw` (exact text) beside the parsed `assertion`. |
| 11.1 | Submit one record | Conforms | `202`; `400`/`401`/`403`/`409`/`500`; response members as listed plus `received_at`. |
| 11.2 | Submit a batch | Conforms | One producer per batch; per-record results; `207`; count and byte limits before crypto. |
| 11.3 | Retrieve one record | Conforms | Domain-scoped; bare id ambiguity returns `409`. |
| 11.4 | Retrieve one execution | Conforms | Receipt order, paged; bare execution id ambiguity returns `409`. Response carries `ingestion.receipt_state`. |
| 12 | Idempotency and errors | Conforms (documented deviation) | Every row implemented. Deviations: replay returns the record's current flags, not the first acceptance result verbatim; a failed insert leaves a `VOID` receipt row and, after a crash, a `PENDING` row for up to `pendingReceiptTimeoutSeconds + receiptRepairIntervalSeconds`. Record and indexes are never visible. |
| 13 | Minimum storage keys and indexes | Conforms | All five keys present. Capability index deferred by the chapter. |
| 14 | Security requirements | Conforms | Limits before crypto; batch limits; no token or body logging; original assertion and `kid` preserved; domain-scoped reads; Lock-derived members rejected from the signed body. TLS is deployment. |
| 15 | Acceptance criteria | Conforms (verified by test run) | `TraceMvpAcceptanceTest` has one or more tests for each of criteria 1–17; all pass. Run 2026-10-14 on the working branch: `mvn -o -pl model,service -am test` gave model 11/11 and service 864/864, 0 failures, 0 skipped (read from the surefire XML). Criterion 12 is asserted as "no record, no `COMMITTED` receipt, no index hit" with `verifyDomain` ok, so it matches the reworded reading in Task 01 item 3, not the literal "no receipt entry" text. Test depth was checked by reading criterion 12 only; other criteria were checked by name and result, not line by line. |
| 16 | Later phases | Deferred | Nothing from later phases present. |

## 2. GovOps profile (secondary source), summary

Only items that touch jans-lock code are listed. Analytics, metrics, capability resolution and
the Producer-Key Authorization Statement (GovOps §9, §10, §13, §14) are deferred by MVP §3,
§6, §7.2 and §13 and are not tracked as work.

| § | Chapter | Status | Notes |
|---|---|---|---|
| 1 | Envelope restructuring | Conforms | Envelope members enforced; `cnf` rejected. |
| 2–5 | `trace`, `subject`, `policy`, `producer_chain` optional members | Conforms | Type-checked when present (`trace.md` "Optional profile members"); `enforcement_mode` closed set; `chain_link` genesis-only. |
| 6 | `parent_record_ids` | Conforms | Typed entries; parents not resolved, which both documents allow. |
| 7, 8, 8.1 | Event kinds, sub-schemas, tokens | Conforms | Invocation outcome vocabulary and operation-binding digests now enforced. |
| 9, 13, 14 | Capability resolution, analytics, metrics | Deferred | By MVP design. |
| 10 | Key registry extensions, authorization statement | Deferred | By MVP §6. |
| 11 | Receipt commitment profile | Conforms | `receipt_profile` is the eighth hash member. Whether that matches `jans-trace-core-mvp-design.md` §7 is Not verified (document unavailable). |
| 12 | Lock-derived members never in the signed body | Conforms | Rejected with `forbidden:<path>` at top level, `trace`, `trace.event` and each fact. Rejection chosen over accept-and-flag. |
| Example D | Non-normative | Document inconsistency | `record_id: "R-capability-invoked-003"` fails MVP §7.1 (UUID/ULID). Also `R-…` ids inside `parent_record_ids` in MVP §7 and GovOps B/D could never be submitted as records. Not a code gap. |

## 3. Remaining work

| Task | Title | Priority |
|---|---|---|
| 01 | Align the MVP design text with the implemented behaviour | Low |

No open code gap against the MVP design remains from this review.
