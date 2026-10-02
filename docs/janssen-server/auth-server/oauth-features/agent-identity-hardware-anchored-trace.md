---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - spiffe
  - agent
  - attestation
  - trace
  - govops
  - design
---

# Hardware-Anchored Agent Identity with TRACE Evidence (Design Plan)

> **Status: design plan, not yet implemented.** This page is the single plan for giving AI agents
> a hardware-anchored identity in jans-auth-server and for producing TRACE evidence about how that
> identity is used. It supersedes and absorbs the earlier
> [Agent Identity via JWT-SVID](./agent-identity-jwt-svid.md) design: everything from that page
> that is still needed is restated here, nothing there is implemented, and this page is the one to
> build from. It follows the
> [Hardware-Anchored Agent Identity Demo Plan](https://github.com/JanssenProject/jans/wiki/Hardware%E2%80%90Anchored-Agent-Identity-Demo-Plan)
> and aligns with the OWASP GovOps architecture (capability as the unit of governance; access
> decisions visible, measurable, accountable). The scope is **jans-auth-server**; changes other
> components need are listed in one short section (section 12) as dependencies.

## 1. The goal in one paragraph

An operator enrolls an AI agent with jans-auth-server. The agent's keypair lives in a hardware
secure element (TPM, TEE, confidential VM) and never leaves it. jans-auth-server **attests** that
key through a dedicated attestation endpoint, binds it to the agent's registration, assigns the
agent a SPIFFE ID, and from then on mints short-lived **JWT-SVIDs** pinned to that key via `cnf`.
Before every issuance the server checks that the hardware passed policy *recently enough*, not only
at enrollment. jans-auth-server publishes its signing keys as a **SPIFFE trust bundle**, so any
relying party, SPIRE or plain JWT verifier, checks the token offline. When a human stands behind the
agent, the human's identity enters through an ID-JAG and is carried into the SVID. Every step the
server takes (appraisal, enrollment, delegation, issuance) is emitted as a signed, hash-chained
**TRACE record** under one execution identifier, so a third party can later correlate the human,
the agent, the hardware key, the authorization decision and the invocation of a governed capability
without calling the server back.

## 2. The trust chain at a glance

Each link answers one question a relying party or an auditor implicitly asks. Break any link and
trust stops there; the chain makes explicit what each party vouches for.

| # | Link | Who | What happens | Artifact |
|---|------|-----|--------------|----------|
| 1 | ROOT | TPM / TEE hardware | Non-exportable keypair; vendor-signed evidence over it (or a platform verifier's signed result) | EAT evidence / attestation result JWT |
| 2 | ATTEST | `/attestation` + script | Single-use challenge; evidence appraised against measurement policy; **assessment stored with a validity window**; verdict signed by jans | `attestation_id`, attestation result JWT |
| 3 | ENROLL | DCR `/register` | Registration accepted only with a fresh `affirming` assessment for the registered key; `client_id` ↔ `jkt` ↔ `attestation_id` persisted; SPIFFE ID assigned | `spiffe://td/agent/<client_id>` |
| 4 | DELEGATE | Token exchange (ID-JAG) | Human's ID token exchanged for an ID-JAG naming the agent; execution id `txn` minted | ID-JAG with `txn` |
| 5 | ISSUE | `/token` + DPoP | `client_credentials` or JWT bearer with ID-JAG; DPoP key must equal the enrolled key **and** have an assessment younger than the maximum age; else `attestation_required` | JWT-SVID with `cnf.jkt`, `txn`, `attestation` |
| 6 | PUBLISH | `/spiffe/bundle` | Signing keys served in SPIFFE bundle format at a stable HTTPS URL | JWKS, `use: jwt-svid` |
| 7 | DECIDE | PDP (AuthZEN / Cedarling) | May agent A (for human H) invoke capability C? | `AUTHORIZATION_DECISION` record |
| 8 | PRESENT & INVOKE | Agent → PEP → capability | SVID + fresh DPoP proof; PEP verifies via the bundle, enforces the decision | `CAPABILITY_INVOKED`, `RUNTIME_EFFECT` records |
| 9 | RECORD & VERIFY | jans-auth-server, PDP, PEP → jans-lock | Each producer signs and chains its records; Lock verifies, receipts, correlates by execution id | One offline-verifiable execution view |

Links 1 to 3 happen once per agent deployment (and link 2 repeats on the attestation cadence).
Links 4 to 9 repeat continuously; tokens live 5 to 15 minutes, so possession of the hardware key,
not any long-lived secret, is the durable credential.

## 3. The correlation model

Every TRACE record jans-auth-server emits carries the same `trace_execution_id`. It is minted once,
when the human's authority first enters the agent flow (the ID-JAG), or at the first SVID issuance
for an autonomous agent, and travels inside every token as the `txn` claim (RFC 8417 §2.2; jans
already uses `txn` in transaction tokens). Everyone downstream (PDP, PEP, capability) copies `txn`
into its own records. Correlation is structural: an auditor queries one execution and gets the
whole chain.

```mermaid
flowchart LR
    subgraph IDS["Identities bound into one execution"]
        H["Human principal<br/>ID token / ID-JAG sub"]
        A["Agent identity<br/>spiffe://td/agent/&lt;client_id&gt;"]
        K["Hardware key<br/>cnf.jkt (DPoP key in TPM / TEE)"]
        P["Policy decision<br/>AuthZEN / Cedarling · ALLOW or DENY"]
        C["Governed capability<br/>capability_id at the PEP"]
    end
    X(("trace_execution_id<br/>= txn claim"))
    H -- "ID-JAG jti" --> X
    A -- "JWT-SVID jti" --> X
    K -- "attestation_id" --> X
    P -- "AUTHORIZATION_DECISION" --> X
    C -- "CAPABILITY_INVOKED / RUNTIME_EFFECT" --> X
    X --> L["jans-lock TRACE store<br/>one execution view"]
```

| Identity | Carried in | Bound by | Recorded as |
|----------|-----------|----------|-------------|
| Human | ID-JAG (`sub`, `email`, `acr`, `auth_time`) | OIDC login at the IdP; token exchange at jans | `subject.principal`; `tokens[]` entry `token_type=id-jag` |
| Agent | JWT-SVID `sub` | Derived from `client_id` under the server's trust domain; never client-supplied | `subject.agent`; `tokens[]` entry `token_type=jwt-svid` |
| Hardware key | JWT-SVID `cnf.jkt`; DPoP proof on every request | Attestation assessment binds `jkt` to platform + measurement | `subject.hardware` (`jkt`, `attestation_id`, `platform`, `measurement`) |
| Policy decision | AuthZEN response | PDP evaluates agent + principal against capability | `AUTHORIZATION_DECISION` by the PDP |
| Capability invocation | PEP log | PEP validates SVID + DPoP, checks decision, invokes | `CAPABILITY_INVOKED` by the PEP; `RUNTIME_EFFECT` by the capability |

jans-auth-server produces the records for the first three rows. The last two are produced by the
PDP and the PEP; this plan only fixes what they must copy (`txn`, token `jti`s, `capability_id`).

## 4. What jans-auth-server already has

This plan is smaller than it looks, because most of the machinery shipped with earlier features.

| Capability | Where | Reused for |
|------------|-------|------------|
| Inbound SPIFFE client auth (draft-ietf-oauth-spiffe-client-auth) | `SpiffeIdUtil`, `SpiffeBundleService` (fetch, cache, stale-serving, negative cache), `SpiffeJwtSvidAssertion`, `spiffe_id` / `spiffe_bundle_endpoint` client metadata, `spiffeTrustDomains` config, `SPIFFE_CLIENT_AUTH` flag | Trust-domain grammar; the bundle consumer doubles as the integration test for the bundle we will serve |
| DPoP (RFC 9449) | `DpopService.getDPoPJwkThumbprint()`, `ExecutionContext.setDpop()`, `cnf.jkt` stamping in `fillPayloadOfAccessTokenJwt()`, `ClientAttributes.dpopBoundAccessToken`, introspection returns `cnf` | Key binding at issuance; assessments are keyed by the DPoP thumbprint |
| JWT access-token minting | `AuthorizationGrant.createAccessToken()` → `createAccessTokenAsJwt()` → `fillPayloadOfAccessTokenJwt()`; `JwtSigner` over `WebKeysConfiguration` | SVID minting branches here |
| DCR + interception scripts | `RegisterCreateAction` / `RegisterUpdateAction`, `RegisterService.updateClientFromRequestObject()`, `ExternalDynamicClientRegistrationService`, `ClientRegistrationType.createClient()` / `updateClient()` (runs after request mapping, before `clientService.persist()`; `false` → `invalid_client_metadata`) | Enrollment gate |
| DCR attestation evidence plumbing | `RegisterRequestParam.EVIDENCE`, `ClientAttributes.evidence`, `AppConfiguration.dcrAttestationEvidenceRequired`, `RegisterValidator.validateEvidence()`, `RegisterErrorResponseType.STALE_EVIDENCE` with `nonce`, `DynamicClientRegistrationContext.getEvidence()` / `createStaleEvidenceWebApplicationException(nonce)` | One-step enrollment; model for the token endpoint's `attestation_required` error |
| Custom script framework | `CustomScriptType`, `BaseExternalType`, `External*Service` pattern, `CdiUtil` | New `attestation` script type |
| ID-JAG (draft-ietf-oauth-identity-assertion-authz-grant) | `IdJagService`, `IdJagValidatorService`, `TokenExchangeService`, `JwtGrantService`, `ExternalIdentityAssertionService`, `identity_assertion_authz_grant` flag | Human identity into the agent flow; `txn` minted here |
| AuthZEN | `AccessEvaluationRestWebServiceImplV1` (`/access/v1/evaluation`), `ExternalAccessEvaluationService` | Decision source when jans is the PDP |
| Introspection (RFC 7662) | `IntrospectionWebService` | Online verification path |
| Feature flags, endpoint registration, config revision | `ErrorResponseFactory.validateFeatureEnabled()`, `ResteasyInitializer`, `Conf.revision` | New endpoints; `spiffe_sequence` |
| Transaction id | `TxTokenService` sets `txn` | Same claim name reused as execution id |
| Audit | `OAuth2AuditLog`, `ApplicationAuditLogger` | Operational log stays; TRACE is a separate signed channel |

## 5. Lifecycle

```mermaid
sequenceDiagram
    autonumber
    participant TEE as TPM / TEE
    participant Agent as AI Agent
    participant Human as Human (OIDC client)
    participant AS as jans-auth-server<br/>(trust domain op.example.org)
    participant Verifier as Platform verifier<br/>(Azure Attestation / Intel TA)
    participant Lock as jans-lock<br/>TRACE store
    participant PEP as PEP / Relying Party

    Note over TEE,AS: ① ATTEST + ENROLL — once per deployment
    Agent->>TEE: generate non-exportable keypair
    Agent->>AS: POST /attestation/challenge { attested_key }
    AS-->>Agent: { challenge_id, nonce }
    Agent->>TEE: evidence over (nonce, public key)
    Agent->>Verifier: evidence (optional, delegated appraisal)
    Verifier-->>Agent: attestation result JWT
    Agent->>AS: POST /attestation { challenge_id, evidence, evidence_format, attested_key }
    Note over AS: core: nonce single-use, key binding<br/>script: signature chain, measurement policy
    AS-->>Lock: ATTESTATION_APPRAISED
    AS-->>Agent: { attestation_id, status: affirming, attestation_result }
    Agent->>AS: DCR /register { jwks: key, access_token_type: JWT-SVID, attestation: attestation_id }
    Note over AS: bind client_id ↔ jkt ↔ attestation_id<br/>assign spiffe://td/agent/#lt;client_id#gt;
    AS-->>Lock: AUTHORIZATION_DECISION (register-client)
    AS-->>Agent: client_id

    Note over AS,PEP: ② PUBLISH — on the bundle's own clock
    PEP->>AS: GET /spiffe/bundle (periodic, or via SPIRE federates_with)
    AS-->>PEP: { keys[use: jwt-svid], spiffe_sequence, spiffe_refresh_hint }

    Note over Human,AS: ③ DELEGATE — when a human starts an execution
    Human->>AS: token exchange: ID token → requested_token_type=id-jag, audience=AS
    AS-->>Lock: AUTHORIZATION_DECISION (issue-id-jag)
    AS-->>Human: ID-JAG { sub: alice, client_id: agent, txn }
    Human->>Agent: hand ID-JAG to the agent

    Note over TEE,AS: ④ ISSUE — every few minutes
    Agent->>TEE: sign DPoP proof (+ private_key_jwt)
    Agent->>AS: POST /token grant=jwt-bearer assertion=ID-JAG (or client_credentials)<br/>DPoP proof · resource=#lt;PEP#gt;
    Note over AS: DPoP key = enrolled key<br/>affirming assessment younger than max age<br/>else 400 attestation_required { nonce }
    AS-->>Lock: AUTHORIZATION_DECISION (issue-jwt-svid)
    AS-->>Agent: JWT-SVID { sub: spiffe id, aud, exp +10m, cnf.jkt, txn, attestation, act }

    Note over TEE,PEP: ⑤ PRESENT & VERIFY — per request
    Agent->>TEE: fresh DPoP proof for the PEP
    Agent->>PEP: request + JWT-SVID + DPoP proof
    Note over PEP: local: signature via bundle, sub, aud, exp<br/>DPoP key ↔ cnf.jkt · PDP decision · copy txn
    PEP-->>Lock: CAPABILITY_INVOKED / RUNTIME_EFFECT
    PEP-->>Agent: 200
```

Steps 1 to 14 establish the hardware binding; if the appraisal is not `affirming` no identity is
ever assigned. Steps 15 to 16 run on the bundle's refresh cadence, never per token. Steps 17 to 20
put a human at the root of the execution. Everything after is steady state: every token is
short-lived, pinned to the hardware key via `cnf`, stamped with the assessment it was issued
under, and recorded.

## 6. Verification: how and where

Three offline-capable paths. In all three the answer to "does the verifier call the auth server?"
is: only periodically for the bundle, never per token.

| Path | Who it's for | How it works |
|------|--------------|--------------|
| **SPIRE federation** | Relying parties in a SPIFFE/SPIRE ecosystem | SPIRE server adds `federates_with` pointing at the jans bundle endpoint (`https_web` profile), fetches on the refresh-hint cadence, distributes to its agents; workloads validate via `ValidateJWTSVID` |
| **Direct bundle fetch** | Ordinary services, gateways | The bundle is a JWKS. Any JWT library verifies the signature and checks `sub`, `aud`, `exp` |
| **Introspection** | High-assurance checks and debugging | RFC 7662 at jans-auth-server for a live answer; the closest thing to revocation the short-TTL model offers |

SPIRE configuration on the relying party side (the entire integration):

```hcl
server {
    trust_domain = "rp-company.example"
    federation {
        federates_with "op.example.org" {
            bundle_endpoint_url = "https://op.example.org/jans-auth/restv1/spiffe/bundle"
            bundle_endpoint_profile "https_web" {}
        }
    }
}
```

**Bearer vs. bound.** Signature + claims checks alone treat the token as bearer. A verifier that
wants the hardware guarantee additionally demands a fresh DPoP proof signed by the key in `cnf`.
Plain SPIFFE verifiers ignore `cnf`, `txn`, `attestation` and `act` (unknown claims are additive),
so the token stays spec-valid for both audiences.

![JWT-SVID verification anatomy](../../../assets/agent-identity-jwt-svid-verification.svg)

| Claim | Check | Roots in |
|-------|-------|----------|
| signature | verifies against a `jwt-svid` key | bundle cached from `/spiffe/bundle` |
| `sub` | `spiffe://op.example.org/agent/3f9c` authorised here? | relying party's identity policy |
| `aud` | equals this service | the service's own identifier |
| `iat`/`exp` | fresh (≤ 15 min) | clock |
| `cnf.jkt` | DPoP proof verifies **and** proof key thumbprint matches | the hardware key, attested |
| `attestation` | `status=affirming`, `appraised_at` recent, `measurement` acceptable to *this* verifier | the appraisal, re-checkable in Lock |
| `act` | which human delegated | ID-JAG, recorded in Lock |

Checks 1 to 4 are standard SPIFFE JWT-SVID validation. Check 5 is the hardware binding. Checks 6
and 7 are optional and give a verifier the hardware state and the human behind the request without
any call to jans.

## 7. Access token type selection: `access_token_type` replaces `access_token_as_jwt`

No new `grant_type`. A grant type answers "on what basis is the client authorised to receive a
token"; for the agent flow that basis is unchanged (`client_credentials`, or JWT bearer with an
ID-JAG). Only the *format* of the issued token changes, and format is a registration-time client
property, the same model jans uses today to choose between bearer and JWT access tokens.

| `access_token_type` | Issued access token |
|---------------------|---------------------|
| `BEARER` | opaque reference token; **default** when absent |
| `JWT` | JWT access token; equivalent of today's `access_token_as_jwt=true` |
| `JWT-SVID` | SPIFFE JWT-SVID |

Rules:

- `access_token_type` present always wins over `access_token_as_jwt`; absent → fall back
  (`JWT` if `true`, else `BEARER`). Existing clients are unaffected.
- `access_token_as_jwt` is deprecated, removal in the next major release. Both are documented in
  swagger (all three client-metadata schema occurrences) during the window; config-api and TUI
  follow (outside this plan).
- `JWT-SVID` constraints at registration and update (`RegisterParamsValidator.validateAccessTokenType()`,
  `400 invalid_client_metadata`): `SPIFFE_SVID_ISSUANCE` flag enabled; `grant_types` ⊆
  {`client_credentials`, `urn:ietf:params:oauth:grant-type:jwt-bearer`}; asymmetric
  `token_endpoint_auth_method` (`private_key_jwt`, `tls_client_auth`, `self_signed_tls_client_auth`);
  `dpop_bound_access_token` not explicitly `false` (it is forced `true`); `jwks` or `jwks_uri`
  present; a valid `attestation` (section 9.5).

Implementation: `io.jans.as.model.common.AccessTokenType` enum (`fromString` accepts `JWT-SVID`
and `JWT_SVID`, case-insensitive); `RegisterRequestParam.ACCESS_TOKEN_TYPE`;
`ClientAttributes.accessTokenType` (stored in `jansAttrs`, no schema change);
`Client.getResolvedAccessTokenType()` / `isAccessTokenJwtFormat()` used everywhere the format is
decided (`AuthorizationGrant.createAccessToken()`, `AbstractAuthorizationGrant.getAccessTokenLifetimeInSeconds()`,
every other `isAccessTokenAsJwt()` reader); `RegisterRequest.accessTokenType` kept as a raw
`String` so an unknown value is rejected rather than ignored; `RegisterJsonService` echoes the
resolved value.

## 8. SPIFFE bundle endpoint (publish)

`GET /jans-auth/restv1/spiffe/bundle`, gated by `FeatureFlagType.SPIFFE_SVID_ISSUANCE`
(`spiffe_svid_issuance`, sibling of `SPIFFE_CLIENT_AUTH`).

```json
{
  "keys": [
    { "kty": "EC", "crv": "P-256", "x": "…", "y": "…", "kid": "7f3a", "alg": "ES256", "use": "jwt-svid" }
  ],
  "spiffe_sequence": 42,
  "spiffe_refresh_hint": 300
}
```

- `SpiffeOwnBundleService` (`@ApplicationScoped`) builds the bundle from
  `WebKeysConfiguration.getKeys()`: keep `use == SIGNATURE` keys with an asymmetric `alg` (never
  HMAC, never `enc`), apply the same `jwksAlgorithmsSupported` filter as `JwkRestWebServiceImpl`
  (extract to a shared helper), emit a minimal RFC 7517 JWK per key (`kty`, `kid`, `alg`,
  `use: "jwt-svid"`, `n`/`e` or `crv`/`x`/`y`; no jans-specific members). `spiffe_sequence` =
  `Conf.revision` (new `ConfigurationFactory.getLoadedRevision()`), `spiffe_refresh_hint` =
  `spiffeBundleRefreshHintSeconds`. An empty key list is a valid bundle.
- `SpiffeBundleRestWebService` / `Impl` in `io.jans.as.server.spiffe.ws.rs`, registered in
  `ResteasyInitializer`; headers `Cache-Control: public, max-age=<refresh hint>`.
- Discovery member `spiffe_bundle_endpoint` (OpenID configuration, client
  `OpenIdConfigurationResponse`, swagger), gated on the same flag.
- **Decision:** reuse the OIDC signing keys for v1 (relabelled `use: jwt-svid` only in the bundle
  output); SVID rotation stays coupled to OIDC rotation. A dedicated key set is a follow-up. During
  rotation the bundle serves old + new keys and bumps `spiffe_sequence`.
- **Trust domain:** `spiffeOwnTrustDomain` is permanent. `getSpiffeOwnTrustDomain()` returns the
  raw configured value; `getEffectiveSpiffeOwnTrustDomain()` applies the issuer-host fallback.
  `SpiffeTrustDomainService.pinTrustDomainIfNeeded()` (from `AppInitializer`) writes the derived
  value into `Conf` on first start with issuance enabled, logs a `WARN` that it is now permanent,
  logs an `ERROR` and continues if no valid trust domain can be derived, and adopts a value another
  node pinned first.
- Dogfood test: a second jans instance's existing `SpiffeBundleService` must accept this bundle.

Configuration: `spiffeOwnTrustDomain` (String), `spiffeBundleRefreshHintSeconds` (300),
`spiffeSvidLifetimeSeconds` (600), `spiffeSvidMaxLifetimeSeconds` (3600). All `@DocProperty`.

## 9. New: the attestation endpoint

### 9.1 Why an endpoint and not only a DCR script

A DCR script proves the key was in hardware *once*. The demo plan asks for "recent attestation for
issuance": the platform must have passed policy recently enough for *this* token. That needs a
challenge that can be issued and consumed outside DCR, an assessment with a validity window that
the token endpoint can look up, and a verdict that is itself evidence. A dedicated endpoint gives
all three; the DCR gate and the token-endpoint rule become callers of one service, so enrollment
and issuance apply exactly one policy.

### 9.2 Flow

```mermaid
sequenceDiagram
    autonumber
    participant TEE as TPM / TEE
    participant Agent as AI Agent
    participant AS as jans-auth-server<br/>/attestation
    participant Script as attestation script
    participant Verifier as Platform verifier
    participant Lock as jans-lock

    Agent->>AS: POST /attestation/challenge { attested_key, client_id?, purpose }
    AS->>Script: createChallenge(context) — optional hook
    AS-->>Agent: 200 { challenge_id, nonce, expires_in }
    Agent->>TEE: evidence over (nonce, public key)
    TEE-->>Agent: EAT / quote
    opt delegated appraisal
        Agent->>Verifier: evidence
        Verifier-->>Agent: attestation result JWT
    end
    Agent->>AS: POST /attestation { challenge_id, evidence, evidence_format, attested_key, client_id? }
    Note over AS: core: challenge valid, single-use<br/>attested_key thumbprint == challenge jkt<br/>client_id owns the key
    AS->>Script: verifyEvidence(context)
    Note over Script: platform-specific: verifier signature,<br/>nonce echoed, key bound, measurement allowed
    Script-->>AS: status · platform · measurement · policy_version
    Note over AS: persist assessment · sign attestation result JWT
    AS-->>Lock: ATTESTATION_APPRAISED (outbox)
    AS-->>Agent: 200 { attestation_id, status, attestation_result, … }
```

The script never sees the nonce lifecycle, the key-binding check or persistence; those are core
and identical for every platform.

### 9.3 Request and response definitions

Base path `/jans-auth/restv1/attestation`. Gated by new `FeatureFlagType.ATTESTATION`
(`attestation`). Both operations are `POST` with `application/json`, need no OAuth client
authentication (the caller may not be registered yet), and are rate-limited per source IP and per
`jkt` like `/register`.

#### `POST /attestation/challenge`

```json
{
  "attested_key": { "kty": "EC", "crv": "P-256", "x": "…", "y": "…" },
  "client_id": "3f9c…",
  "purpose": "enroll"
}
```

| Field | Required | Meaning |
|-------|----------|---------|
| `attested_key` | yes; JWK **or** `jkt` string | Public key the evidence will be about. Core computes the RFC 7638 thumbprint; everything downstream is keyed by it |
| `client_id` | no | Re-attesting an existing client. Core checks the key is one of the client's `jwks` / `jwks_uri` keys (same lookup as section 10.3) |
| `purpose` | no | `enroll` (default without `client_id`) or `issue`; lets the script pick a stricter policy for issuance |

Response `200`:

```json
{
  "challenge_id": "c0a8d4…",
  "nonce": "qH3kZ9…",
  "expires_in": 300,
  "evidence_formats_supported": ["azure-maa+jwt", "intel-ta+jwt", "eat+jwt", "tpm2-quote+json"]
}
```

Nonce ≥ 128 bits random, stored with `{ jkt, client_id, purpose, expires_at }` under
`challenge_id` in `CacheService` (TTL `attestationChallengeLifetimeSeconds`), single-use. A script
may add response members via `modifyChallengeResponse`.

Errors (`400`): `invalid_request` (malformed key), `invalid_client` (unknown `client_id`),
`key_not_registered`.

#### `POST /attestation`

```json
{
  "challenge_id": "c0a8d4…",
  "evidence": "eyJhbGciOiJSUzI1NiIs…",
  "evidence_format": "azure-maa+jwt",
  "attested_key": { "kty": "EC", "crv": "P-256", "x": "…", "y": "…" },
  "client_id": "3f9c…"
}
```

| Field | Required | Meaning |
|-------|----------|---------|
| `challenge_id` | yes | From `/attestation/challenge`; consumed on use |
| `evidence` | yes | Opaque to core: compact JWT for `*+jwt`, base64url CBOR for `eat+cwt`, JSON document for `tpm2-quote+json`. Max `attestationMaxEvidenceBytes` |
| `evidence_format` | yes | Selects the script branch; must be in `attestationEvidenceFormatsSupported` |
| `attested_key` | yes | Must thumbprint to the challenge `jkt` |
| `client_id` | conditional | Must equal the challenge's `client_id` when one was given |

Core, before the script: flag; challenge exists, unexpired, unconsumed (consumed atomically);
thumbprint equals challenge `jkt`; `client_id` consistency; size and format. Then
`verifyEvidence(context)`. Core, after the script: `status` set; if `affirming`, the script's
`attested_jkt` equals the challenge `jkt` and the script confirmed the nonce was echoed; otherwise
downgraded to `contraindicated` with `key_mismatch` / `nonce_mismatch`.

Response `200` (also for negative verdicts; the verdict is the payload):

```json
{
  "attestation_id": "att_7b1e…",
  "status": "affirming",
  "attested_jkt": "9XKt…",
  "platform": "azure-cvm-sev-snp",
  "measurement": "sha384:c9e4…",
  "policy_version": "agent-tpm-policy-2026-09",
  "appraised_at": 1790000000,
  "expires_at": 1790000900,
  "verifier": "https://sharedeus.eus.attest.azure.net",
  "reasons": [],
  "attestation_result": "eyJhbGciOiJFUzI1NiIsImtpZCI6Ij…",
  "trace_record_id": "rec_01J…"
}
```

| Field | Meaning |
|-------|---------|
| `attestation_id` | Primary key of the stored assessment; used in DCR and `/token` (`attestation` parameter) and in TRACE |
| `status` | RATS EAR vocabulary (draft-ietf-rats-ar4si; same as TRACE `appraisal.status`): `affirming`, `warning`, `contraindicated`. Only `affirming` satisfies enrollment and issuance unless `attestationAcceptWarning=true` |
| `attested_jkt` | RFC 7638 thumbprint of the attested key as confirmed by the evidence |
| `platform` | TRACE `runtime.platform` value (`tpm2`, `azure-cvm-sev-snp`, `intel-tdx`, `amd-sev-snp`, `aws-nitro`, `software-only`, …) |
| `measurement` | `sha256:…` / `sha384:…` (TPM PCR composite, TDX MRTD, SEV launch measurement) |
| `policy_version` | Identifier of the measurement / platform policy the script applied |
| `appraised_at`, `expires_at` | Validity window; length `attestationAssessmentLifetimeSeconds` unless the script shortens it |
| `verifier` | URI of the party that appraised raw evidence: the platform verifier for delegated formats, this issuer for in-process formats |
| `reasons` | Stable codes for `warning` / `contraindicated`: `measurement_not_allowed`, `evidence_expired`, `nonce_mismatch`, `key_mismatch`, `untrusted_verifier`, `debug_enabled`, … |
| `attestation_result` | The verdict as a JWT signed by this server (9.6) |
| `trace_record_id` | `record_id` of the `ATTESTATION_APPRAISED` TRACE record when emission is enabled |

Errors (`400`): `invalid_challenge` (unknown / expired / used; body carries a fresh `challenge_id`
+ `nonce` for a one-round-trip retry), `invalid_request`, `unsupported_evidence_format`,
`evidence_too_large`. `500` is never used for a negative verdict.

### 9.4 The interception script: `attestation`

New `CustomScriptType.ATTESTATION("attestation", "Attestation", AttestationType.class, …)` in
jans-core with `io.jans.model.custom.script.type.attestation.AttestationType` and
`DummyAttestationType`; `ExternalAttestationService` in jans-auth-server following the
`ExternalAccessEvaluationService` pattern (script selected by `attestationScriptName`, or enabled
scripts tried in `level` order until one claims the `evidence_format`).

```java
public interface AttestationType extends BaseExternalType {

    /** Optional: customise the challenge (nonce encoding, TTL, extra response members). */
    boolean createChallenge(Object attestationContext);

    /** Mandatory: appraise evidence; fill AttestationResult on the context. Return false to reject outright. */
    boolean verifyEvidence(Object attestationContext);

    /** Optional: add/remove members of the /attestation response. */
    boolean modifyResponse(Object responseAsJsonObject, Object attestationContext);
}
```

`AttestationContext` (`io.jans.as.server.service.external.context`) exposes `httpRequest`,
`appConfiguration`, `script` (`configurationAttributes`), `challenge` (nonce, `jkt`, `client_id`,
`purpose`), `client` (nullable), `evidenceFormat`, `evidenceRaw`, `evidenceJwt` (parsed for
`*+jwt`), `evidenceJson`, and a mutable `AttestationResult` (`status`, `attestedJwk`,
`nonceVerified`, `platform`, `measurement`, `policyVersion`, `verifier`, `reasons`,
`lifetimeSeconds`, `claims` copied into the result token). Helpers:
`verifyJwtAgainstJwks(jwt, jwksUri)`, `thumbprint(jwk)`, `cacheGet` / `cachePut`.

Reference script `docs/script-catalog/attestation/AgentAttestation.py` (+ `.md`):

| Attribute | Purpose |
|-----------|---------|
| `formats` | Comma list handled, e.g. `azure-maa+jwt,eat+jwt` |
| `trusted_verifier_jwks_uri` | JWKS of the verifier whose result JWTs are accepted (Azure Attestation `…/certs`, Intel TA JWKS, test JWKS in CI) |
| `trusted_verifier_issuers` | Allowed `iss` values |
| `nonce_claim_path`, `attested_key_claim_path`, `measurement_claim_path`, `platform_claim_path` | Dotted claim paths; verifiers differ (Azure: `x-ms-runtime.keys`, `x-ms-sevsnpvm-launchmeasurement`; Intel TA: `tdx_mrtd`, `nonce`) |
| `allowed_measurements` | Allowlist; empty means "record, do not enforce" → `warning`, never `affirming` |
| `policy_version` | Echoed into the result; bump when the allowlist changes |
| `require_debug_disabled` | SEV-SNP `debug` flag, TDX `debug` attribute |

Delegate, don't parse: v1 verifies a platform verifier's signed *result*. In-process TPM 2.0 quote
verification (`tpm2-quote+json`) is a second script, after v1.

### 9.5 Where the gate is enforced

**At DCR.** Attestation is required when any of: `access_token_type=JWT-SVID`;
`dcrAttestationEvidenceRequired=true` (existing property, now meaning "every registration"); the
request carries `attestation` or `evidence`. Two equivalent paths end in
`AttestationService.verifyForEnrollment()`:

- *Two-step (preferred).* Agent calls `/attestation/challenge` + `/attestation`, then registers
  with new `RegisterRequestParam.ATTESTATION` (`attestation`: an `attestation_id` or the
  `attestation_result` JWT). `RegisterValidator.validateAttestation()` (next to
  `validateEvidence()`) requires `status=affirming`, `expires_at` in the future, `purpose=enroll`,
  and `attested_jkt` equal to the thumbprint of one key in `jwks` / `jwks_uri`.
- *One-step (existing `evidence`).* First request without evidence gets `400 stale_evidence` with
  `nonce` **and** `challenge_id` (existing error, one member added); the second request carries
  `evidence` + `evidence_format` and is routed through `AttestationService.verify()` exactly like
  `/attestation`, then continues as above.

On success: `ClientAttributes.attestation = { attestationId, jkt, platform, measurement,
policyVersion, appraisedAt }`; `clientId` set on the assessment; `dpopBoundAccessToken` forced
`true`; enrollment TRACE record emitted. Existing `ClientRegistrationType` scripts still run after
the gate and can read `context.getAttestationAssessment()`. `updateClient` re-runs the gate when
`jwks` / `jwks_uri` change; a key swap without a fresh assessment for the new key is rejected
(`invalid_client_metadata`, `attestation_required_for_new_key`).

**At `/token`** (section 10.4).

### 9.6 Attestation result token

JWT signed with the server's OIDC signing key, `typ: "attestation-result+jwt"`:

```json
{
  "iss": "https://op.example.org",
  "sub": "9XKt…",
  "jti": "att_7b1e…",
  "iat": 1790000000,
  "exp": 1790000900,
  "status": "affirming",
  "platform": "azure-cvm-sev-snp",
  "measurement": "sha384:c9e4…",
  "policy_version": "agent-tpm-policy-2026-09",
  "verifier": "https://sharedeus.eus.attest.azure.net",
  "client_id": "3f9c…",
  "nonce": "qH3kZ9…",
  "evidence_digest": "sha256:…"
}
```

`sub` is the attested `jkt`, `jti` is the `attestation_id`. It is the RATS "passport": a relying
party checks signature, `exp`, `status`, and that `sub` equals the thumbprint of the key the caller
is proving possession of.

### 9.7 Persistence

New entity `AttestationAssessment` (`ou=attestations`, `jansAttest`): `attestationId` (primary),
`jkt` (indexed), `clientId` (indexed, nullable until enrollment), `status`, `platform`,
`measurement`, `policyVersion`, `verifier`, `evidenceFormat`, `evidenceDigest`, raw evidence only
when `attestationStoreEvidence=true`, `appraisedAt`, `expiresAt` (ORM TTL attribute, as tokens),
`traceRecordId`, `reasons`. Schema in `jans-linux-setup`, ORM entries for all backends. Lookups:
`latestAffirming(clientId, jkt)`, `byId`.

### 9.8 Configuration and discovery

| Property | Default | Meaning |
|----------|---------|---------|
| `attestationChallengeLifetimeSeconds` | 300 | Challenge TTL |
| `attestationAssessmentLifetimeSeconds` | 900 | Default `expires_at - appraised_at` |
| `attestationMaxAgeForIssuanceSeconds` | 300 | Oldest `appraised_at` accepted at `/token`; a client attribute `attestationMaxAgeSeconds` may lower it |
| `attestationRequireFreshPerIssuance` | `false` | Every issuance consumes an assessment |
| `attestationAcceptWarning` | `false` | Accept `warning` verdicts |
| `attestationEvidenceFormatsSupported` | `["azure-maa+jwt","intel-ta+jwt","eat+jwt"]` | Advertised and enforced |
| `attestationMaxEvidenceBytes` | 65536 | |
| `attestationStoreEvidence` | `false` | Keep raw evidence |
| `attestationScriptName` | — | Pin one script |

Discovery members (gated on the flag): `attestation_endpoint`, `attestation_challenge_endpoint`,
`attestation_evidence_formats_supported`.

## 10. JWT-SVID issuance at `/token`

### 10.1 Request

```text
POST /jans-auth/restv1/token
DPoP: <proof JWT signed by the hardware key>

grant_type=client_credentials
&client_id=3f9c…
&client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
&client_assertion=<private_key_jwt signed by the hardware key>
&resource=https://rp.example.com
```

or, with a human behind the agent:

```text
grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer
&assertion=<ID-JAG>
&resource=https://rp.example.com
```

### 10.2 Validation

In `TokenRestWebServiceImpl.requestAccessToken()` after `validateGrantType`, when
`client.getResolvedAccessTokenType() == JWT_SVID`, call new
`TokenRestWebServiceValidator.validateJwtSvidRequest(gt, client, request, dpopJkt, auditLog)`:

| Check | Error |
|-------|-------|
| `gt` not `client_credentials` or JWT bearer | `400 unsupported_grant_type` |
| `SPIFFE_SVID_ISSUANCE` disabled | `400 invalid_request` |
| No `resource`, or a value is not an absolute URI without fragment (RFC 8707 §2) | `400 invalid_target` (new `TokenErrorResponseType.INVALID_TARGET`) |
| No DPoP thumbprint | already `invalid_dpop_proof` because `dpopBoundAccessToken` is forced |
| DPoP key not a registered key (10.3) | `400 invalid_dpop_proof` |
| No fresh affirming assessment for `(client_id, dpopJkt)` (10.4) | `400 attestation_required` |

`resource` is read with `request.getParameterValues("resource")` (as `TokenExchangeService` does)
and carried on `ExecutionContext` (new `List<String> resources`).

### 10.3 Key binding rule

New `DpopService.validateDpopKeyIsRegistered(Client, String jkt)`: load the client's JWKS with
`CommonUtils.getJwks(client)` (handles `jwks` and `jwks_uri`, as `MTLSService` does), compute
`JSONWebKey.getJwkThumbprint()` for each key, require the proof thumbprint to be one of them, else
`400 invalid_dpop_proof` ("DPoP proof key is not a registered key of this client"). This is what
makes "the DPoP key equals the enrolled key" true; the attestation gate then proves that same key
lives in hardware.

### 10.4 Attestation freshness rule

```text
assessment = attestationService.latestAffirming(client_id, dpopJkt)
maxAge     = min(attestationMaxAgeForIssuanceSeconds, client.attestationMaxAgeSeconds)
if assessment == null
   or assessment.appraisedAt < now - maxAge
   or (attestationRequireFreshPerIssuance && assessment already consumed by an issuance)
then 400 attestation_required
```

```json
{
  "error": "attestation_required",
  "error_description": "No affirming attestation younger than 300 seconds for the DPoP key.",
  "challenge_id": "c0a8d4…",
  "nonce": "qH3kZ9…",
  "expires_in": 300
}
```

New `TokenErrorResponseType.ATTESTATION_REQUIRED("attestation_required")`. The agent satisfies it
in one of three ways, in order of preference:

1. Re-attest out of band (`/attestation/challenge` with `client_id`, `purpose=issue`, then
   `/attestation`), retry `/token`; the assessment is found by `(client_id, jkt)`.
2. Inline `attestation=<attestation_id | attestation_result JWT>` on the token request; verified
   exactly as at DCR, and `attested_jkt` must equal the DPoP proof key.
3. Attest-and-issue in one call: `evidence`, `evidence_format` and the `challenge_id` from the
   error on the token request; core runs `AttestationService.verify()` inline. This keeps the
   "fresh assessment for every issuance" mode at two round trips.

### 10.5 Minting

`AuthorizationGrant.createAccessToken()` branches on the resolved type:

```java
JwtSigner jwtSigner = null;
switch (getClient().getResolvedAccessTokenType()) {
    case JWT:      jwtSigner = createAccessTokenAsJwt(accessToken, context); break;
    case JWT_SVID: jwtSigner = jwtSvidBuilder.build(this, accessToken, context); break;
    default:       /* BEARER: opaque */
}
```

New `io.jans.as.server.model.token.JwtSvidBuilder` produces:

```json
{
  "iss": "https://op.example.org",
  "sub": "spiffe://op.example.org/agent/3f9c…",
  "aud": "https://rp.example.com",
  "iat": 1790000100,
  "exp": 1790000700,
  "jti": "svid_…",
  "cnf": { "jkt": "9XKt…" },
  "txn": "exe_01J9…",
  "attestation": {
    "id": "att_7b1e…",
    "status": "affirming",
    "platform": "azure-cvm-sev-snp",
    "measurement": "sha384:c9e4…",
    "policy_version": "agent-tpm-policy-2026-09",
    "appraised_at": 1790000000
  },
  "act": { "sub": "alice", "iss": "https://idp.example.com" }
}
```

| Part | Value | Rule |
|------|-------|------|
| header `alg`, `kid`, `typ: JWT` | client `access_token_signing_alg` if asymmetric else `defaultSignatureAlgorithm`; key from `WebKeysConfiguration` (the set the bundle publishes); HMAC rejected at registration and defensively here | existing `JwtSigner` |
| `iss` | `appConfiguration.getIssuer()` | |
| `sub` | `SpiffeIdUtil.buildAgentSpiffeId(trustDomain, clientId)` → `spiffe://<td>/agent/<client_id>`; validates the trust domain grammar first (a `/` in the configured domain must not silently become path segments) | **derived**, never from the `spiffe_id` attribute, which means the opposite thing (an external identity the client authenticates *in* with). Spoofing and collisions are structurally impossible |
| `aud` | exactly the validated `resource` values; `removeClaim` first so neither `client_id` nor `additionalAudience` leak in | **required**; the reverse of the inbound validator where `aud` must equal our issuer |
| `iat`, `nbf`, `exp` | from `accessToken`; lifetime per 10.6 | |
| `jti` | `context.getTokenReferenceId()` | |
| `cnf.jkt` | `context.getDpop()` | always present here |
| `txn` | the ID-JAG's `txn` for a JWT bearer grant; else the `txn` request parameter if the client may continue executions; else freshly minted | correlation key |
| `attestation` | the assessment that satisfied 10.4 | hardware state the token was issued under |
| `act` | ID-JAG `sub` / `iss`; omitted for pure `client_credentials` | human ↔ agent correlation inside the token |
| `client_id`, `scope`, `token_type` | as today | harmless additive claims |

Everything else in `createAccessToken()` (`externalUpdateTokenService.modifyAccessToken()`,
`statusListService`, `TokenEntity` persistence, stats) is unchanged, so introspection, revocation
and the update-token script keep working. The update-token script may add claims to an SVID but may
not remove `sub`, `aud`, `cnf`, `txn`, `attestation` or `act` (predictability for verifiers).

### 10.6 Lifetime

`AbstractAuthorizationGrant.getAccessTokenLifetimeInSeconds()`: for `JWT_SVID` start from
`spiffeSvidLifetimeSeconds` instead of `accessTokenLifetime`, honour the client-specific lifetime
and the update-token override, clamp to `spiffeSvidMaxLifetimeSeconds`, and keep the
key-regeneration adjustment (an SVID must not outlive its signing key's presence in the bundle).

### 10.7 Response and introspection

Token response shape is unchanged: `access_token` = the SVID, `token_type: DPoP`, `expires_in`, no
refresh token. `IntrospectionWebService` returns `sub` = the SPIFFE ID, `cnf.jkt`, `txn`,
`attestation` and `act` for SVID grants.

### 10.8 `txn` in the ID-JAG

`IdJagService` sets `txn` on every ID-JAG it issues (fresh identifier unless the exchange request
carries `txn` and the client is flagged `idJagAllowTxnContinuation`). This single change puts the
human's login at the root of the execution: the ID-JAG `jti` and `sub` appear in `tokens[]` and
`subject.principal` of every later record.

## 11. TRACE evidence from jans-auth-server

### 11.1 Producer model

jans-auth-server becomes a TRACE **producer** towards jans-lock, which already ships the evidence
store, verifier and admin API (profile `tag:jans.io,2026:trace-v1`; event kinds
`AUTHORIZATION_DECISION`, `CAPABILITY_INVOKED`, `RUNTIME_EFFECT`; envelope `producer`, `kid`,
`record_id`, `trace`, `producer_chain`, `parent_record_ids`, `signature`; Ed25519 producer keys
registered through `/audit/trace/admin/producer-keys`). Records are signed with a **dedicated
Ed25519 producer key** (not the OIDC keys: producer keys are registered and revoked in Lock's
registry, and revocation is anchored to chain position). Each node keeps its own chain
(`producer_instance_id` = node id, monotonic `sequence_number`, `prev_record_hash`, zero hash
first).

```mermaid
flowchart LR
    subgraph AS["jans-auth-server (producer: jans-auth)"]
        E1["ATTESTATION_APPRAISED<br/>subject.hardware · verdict"]
        E2["AUTHORIZATION_DECISION<br/>urn:jans:capability:register-client"]
        E3["AUTHORIZATION_DECISION<br/>urn:jans:capability:issue-id-jag"]
        E4["AUTHORIZATION_DECISION<br/>urn:jans:capability:issue-jwt-svid<br/>tokens: id-jag, jwt-svid"]
    end
    subgraph PDP["PDP (producer: cedarling)"]
        E5["AUTHORIZATION_DECISION<br/>payments:refund · ALLOW"]
    end
    subgraph PEP["Gateway (producer: demo-gateway)"]
        E6["CAPABILITY_INVOKED<br/>tokens: jwt-svid"]
        E7["RUNTIME_EFFECT<br/>result_digest"]
    end
    E1 -- "prev_record_hash" --> E2 -- "prev_record_hash" --> E3 -- "prev_record_hash" --> E4
    E4 -. "parent: issued_credential" .-> E5
    E5 -. "parent: authorized_by" .-> E6
    E6 -. "parent: produced_effect" .-> E7
    L[("jans-lock<br/>same trace_execution_id")]
    E1 & E2 & E3 & E4 & E5 & E6 & E7 --> L
```

Solid arrows: the per-producer hash chain Lock verifies. Dotted arrows: `parent_record_ids`
cross-references. Everything sharing one `trace_execution_id` (= `txn`) is one execution.

### 11.2 Records emitted

| Event | `event_kind` | When | `capability_ids` | `tokens[]` | `subject` |
|-------|--------------|------|------------------|------------|-----------|
| Attestation appraised | `ATTESTATION_APPRAISED` (new kind in Lock, section 12; fallback: `AUTHORIZATION_DECISION` with `urn:jans:capability:attest-key`) | every verdict | none | none | `hardware { jkt, attestation_id, platform, measurement, policy_version, status, verifier }` |
| Client enrolled | `AUTHORIZATION_DECISION` | DCR accepted / rejected for an attested client | `urn:jans:capability:register-client` | none | `agent { spiffe_id, client_id }`, `hardware` |
| Human delegated | `AUTHORIZATION_DECISION` | ID-JAG issued | `urn:jans:capability:issue-id-jag` | `id-jag` (`jti`), `id_token` (`fingerprint`) | `principal { iss, sub, acr, auth_time }`, `agent { client_id }` |
| SVID issued | `AUTHORIZATION_DECISION` | SVID issued or refused | `urn:jans:capability:issue-jwt-svid` | `jwt-svid` (`jti`), `id-jag` when present | `agent`, `principal`, `hardware` |

SVID issuance record (`trace.policy` describes jans's own issuance policy; `runtime.pdp_id` is this
server because the auth server is the PDP for "may this agent receive this credential"):

```json
{
  "producer": "jans-auth",
  "kid": "jans-auth-trace-2026q4",
  "record_id": "rec_01J9…",
  "trace": {
    "eat_profile": "tag:jans.io,2026:trace-v1",
    "event_kind": "AUTHORIZATION_DECISION",
    "signed_at": 1790000100,
    "trace_execution_id": "exe_01J9…",
    "execution_authority": "https://op.example.org",
    "subject": {
      "agent": { "spiffe_id": "spiffe://op.example.org/agent/3f9c…", "client_id": "3f9c…" },
      "principal": { "iss": "https://idp.example.com", "sub": "alice", "acr": "urn:…:mfa" },
      "hardware": { "jkt": "9XKt…", "attestation_id": "att_7b1e…", "platform": "azure-cvm-sev-snp",
                    "measurement": "sha384:c9e4…", "policy_version": "agent-tpm-policy-2026-09" }
    },
    "policy": {
      "policy_store_id": "jans-auth-issuance",
      "policy_store_version": "agent-tpm-policy-2026-09",
      "policy_language": "jans-config",
      "policy_language_version": "1",
      "bundle_hash": "sha256:…"
    },
    "runtime": { "pdp_id": "https://op.example.org" },
    "event": {
      "outcome": "ALLOW",
      "capability_ids": [ { "capability_id": "urn:jans:capability:issue-jwt-svid", "outcome": "ALLOW" } ],
      "tokens": [
        { "issuer": "https://op.example.org", "token_type": "jwt-svid", "jti": "svid_…" },
        { "issuer": "https://idp.example.com", "token_type": "id-jag", "jti": "550e…" }
      ],
      "audience": "https://rp.example.com",
      "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer"
    }
  },
  "producer_chain": {
    "producer_id": "jans-auth",
    "producer_instance_id": "node-a",
    "producer_chain_id": "jans-auth/node-a/2026q4",
    "sequence_number": 4182,
    "prev_record_hash": "sha256:…"
  },
  "parent_record_ids": [
    { "producer_id": "jans-auth", "record_id": "rec_attest…", "relationship_type": "attested_by" }
  ],
  "signature": "…"
}
```

`bundle_hash` is the SHA-256 of the canonical JSON of the issuance-relevant configuration
(`attestationMaxAgeForIssuanceSeconds`, `attestationAcceptWarning`, `spiffeSvidLifetimeSeconds`,
the script's `allowed_measurements` and `policy_version`), so a configuration change shows up as a
policy change. Token refs never carry token material, only `jti` or a SHA-256 `fingerprint`, as
Lock's validator enforces.

### 11.3 Emission path

New package `io.jans.as.server.trace`:

- `TraceRecordBuilder`: envelope, RFC 8785 canonicalisation (move Lock's `JcsCanonicalizer` to
  jans-core), Ed25519 signature with key alias `trace-producer` from `AuthCryptoProvider`
  (generated at startup when missing, rotated manually).
- `TraceChainState`: persistent per-node `{ chain_id, sequence_number, prev_record_hash }`
  (`jansTraceChain`), updated in the same transaction the record is queued, so a crash cannot fork
  the chain.
- `TraceEmitter`: durable outbox (`jansTraceOutbox`) + timer posting to Lock `POST /audit/trace`
  with a `trace.write` client-credentials token from this server; retry with backoff; never drops
  silently. `traceEmissionMode=inline` posts before the HTTP response (demo mode).
- Hooks: `AttestationService.verify()`, `RegisterCreateAction` / `RegisterUpdateAction` (attested
  clients), `IdJagService`, `TokenRestWebServiceImpl.requestAccessToken()` for `JWT-SVID` clients
  (both outcomes). Other OAuth traffic produces no records unless `traceRecordAllIssuance=true`.

| Property | Default | Meaning |
|----------|---------|---------|
| `traceEnabled` | `false` | Master switch |
| `traceLockServerUrl` | — | Lock base URL |
| `traceProducerId` | `jans-auth` | Lock `producer_id`; must be allowed by Lock's `clientDomainBindings` |
| `traceProducerKeyAlias` | `trace-producer` | Keystore alias |
| `traceEvidenceDomainId` | — | Lock evidence domain |
| `traceClientId` | — | Client used to obtain `trace.write` tokens |
| `traceEmissionMode` | `outbox` | `outbox` or `inline` |
| `traceRecordAllIssuance` | `false` | Also record non-SVID issuance |
| `traceAttestationAsDecision` | `false` | Fallback encoding until Lock has `ATTESTATION_APPRAISED` |

### 11.4 Mapping to the agentrust TRACE v0.2 record

Lock's profile is an event model; the agentrust Trust Record is a per-run summary. A gateway that
publishes an agentrust record for a run reads one Lock execution and fills: `subject` ←
`subject.agent.spiffe_id`; `cnf.jwk` ← the attested key; `runtime.platform` / `measurement` /
`nonce` and `appraisal.*` ← `ATTESTATION_APPRAISED`; `policy.bundle_hash` ← the PDP decision;
`tool_transcript.hash` ← digest over the ordered `CAPABILITY_INVOKED` records;
`references[rel=authorized-intent]` ← the PDP decision record; `references[rel=approval-outcome]`
← the ID-JAG (`jti`, IdP as resolver); `delegation.credential_id` ← the ID-JAG `jti`;
`transparency` ← Lock receipt URI. Export concern, not auth-server code; listed so the demo can
show both views from one set of facts.

## 12. What's new inside jans-auth-server

```mermaid
flowchart LR
    subgraph AS["jans-auth-server"]
        CH["/attestation/challenge"]:::new
        AT["/attestation"]:::new
        ASVC["AttestationService<br/>nonce · key binding · store · result JWT"]:::new
        ASCR["attestation script<br/>ExternalAttestationService"]:::new
        ASTORE["AttestationAssessment store"]:::new
        DCR["DCR /register<br/>access_token_type · validateAttestation"]:::new
        TOK["/token<br/>validateJwtSvidRequest · key rule · freshness rule"]:::new
        DPOP["DPoP validator<br/>RFC 9449 · ships today"]
        IDJ["ID-JAG<br/>+ txn"]:::new
        MINT["JwtSvidBuilder<br/>sub derived · aud from resource · cnf · txn · attestation · act"]:::new
        KEYS["Signing keys<br/>WebKeysConfiguration"]
        BUN["/spiffe/bundle<br/>SpiffeOwnBundleService"]:::new
        TR["TraceRecordBuilder · TraceChainState · TraceEmitter"]:::new
        INTRO["Introspection<br/>+ SPIFFE sub · cnf · txn · attestation · act"]
    end
    AGENT["AI agent<br/>(TPM / TEE key)"]
    HUMAN["Human via OIDC client"]
    VER["Platform verifier"]
    LOCK["jans-lock TRACE"]
    SPIRE["SPIRE / any JWT verifier"]
    PEP["PEP → governed capability"]

    AGENT -- "1 challenge · 2 evidence" --> CH & AT
    AGENT -. "evidence" .-> VER
    AT --> ASVC --> ASCR
    ASVC --> ASTORE
    AGENT -- "3 register" --> DCR --> ASVC
    HUMAN -- "4 ID token → ID-JAG" --> IDJ
    AGENT -- "5 /token + DPoP" --> TOK --> DPOP --> MINT
    TOK --> ASVC
    MINT --> KEYS --> BUN
    ASVC & DCR & IDJ & TOK --> TR --> LOCK
    SPIRE & PEP -- "fetch periodically" --> BUN
    AGENT -- "6 SVID + DPoP" --> PEP
    PEP -. "online check" .-> INTRO

    classDef new stroke:#0f766e,stroke-width:2.5px;
```

Deliberately absent: a CA or X.509-SVID issuance; the SPIFFE Workload API (agents are ordinary
OAuth clients over HTTPS); in-process parsing of SEV-SNP / TDX quotes; any dependency on agentrust
SDKs.

### Dependencies outside jans-auth-server

| Component | Change | Needed by |
|-----------|--------|-----------|
| jans-core | `CustomScriptType.ATTESTATION`, `AttestationType`, `DummyAttestationType`; `JcsCanonicalizer` moved from Lock | PR 1, PR 6 |
| jans-lock | New event kind `ATTESTATION_APPRAISED` + validator (`outcome` ∈ affirming/warning/contraindicated, `subject.hardware.jkt`, `event.attestation_id`, `platform`, `measurement`, `policy_version`; forbids `capability_ids`, `policy`); confirm `POST /audit/trace` submit and `GET /audit/trace/executions/{id}` read endpoints (documented, only the admin REST class exists on this branch) | PR 6, demo |
| jans-linux-setup | Schema for `jansAttest`, `jansTraceChain`, `jansTraceOutbox`; register `jans-auth` producer key + chain in Lock when both are enabled; ship the reference script | PR 1, PR 6 |
| jans-config-api, jans-cli-tui, Admin UI | `access_token_type`, attestation and trace properties | after PR 2 |
| PDP / PEP (Cedarling, demo gateway) | Copy `txn`, token `jti`s and `capability_id` into their records; PEP validates SVID + DPoP via the bundle | demo |

## 13. Implementation phases and pull requests

Each PR updates `jans-auth-server/docs/swagger.yaml` and the docs it touches in the same change.

| # | PR | Scope |
|---|----|-------|
| 1 | `access_token_type` | Section 7: enum, `RegisterRequestParam`, `ClientAttributes.accessTokenType`, resolver, validator (without the attestation row), swagger deprecation of `access_token_as_jwt`, `RegisterRequest` client support |
| 2 | Bundle endpoint | Section 8: `SPIFFE_SVID_ISSUANCE` flag, `SpiffeOwnBundleService`, endpoint, discovery member, trust-domain pinning, config properties, swagger path + `SpiffeBundle` schema |
| 3 | Attestation endpoint | Section 9: `ATTESTATION` flag, `/attestation/challenge`, `/attestation`, `AttestationService`, entity + schema, result JWT, script type + `ExternalAttestationService`, reference script, discovery members, swagger |
| 4 | DCR gate | Section 9.5: `attestation` parameter, `validateAttestation()`, `stale_evidence` + `challenge_id`, one-step `evidence` routing, `ClientAttributes.attestation`, `updateClient` re-gate, attestation row in the `JWT-SVID` validator, swagger client-metadata schemas |
| 5 | Issuance | Section 10: `validateJwtSvidRequest`, `invalid_target`, key rule, freshness rule + `attestation_required`, inline `attestation` / `evidence` / `challenge_id` parameters, `JwtSvidBuilder`, lifetime, introspection, `txn` in ID-JAG, `TokenRequest` client support (`setResource`, `setAttestation`), swagger token endpoint |
| 6 | TRACE emission | Section 11: producer key, chain state, outbox, emitter, four record builders, configuration, setup step |
| 7 | Interop and demo | SPIRE federation test (docker-compose recipe, manual before releases); end-to-end demo on one hardware platform; the wiki's failure demonstrations; agentrust export (11.4) |

### Hardware platform for the demo

Pick one and document its exact guarantee. Recommendation: **Azure confidential VM (AMD SEV-SNP
with vTPM)** with **Azure Attestation** as verifier. The verifier returns a signed JWT (one script
branch, no quote parsing); the result covers the SEV-SNP launch measurement and the vTPM-held key
(`x-ms-runtime.keys`), so `platform=azure-cvm-sev-snp`; the agent key is generated in the vTPM and
is non-exportable. What it proves: the key is in a vTPM inside a VM whose launch measurement
Azure's verifier appraised. What it does not prove: anything about the process that asks the vTPM
to sign (the demo plan's stated limitation). Fallback: a plain **TPM 2.0** host (`platform=tpm2`),
which needs the in-process quote script and an AK certificate chain to a manufacturer CA.

## 14. Test design

Project rules apply: TestNG + Mockito, `@Listeners(MockitoTestNGListener.class)`, `lenient()` for
shared stubs, a test class is named after the class under test plus `Test`, all `@Test` methods
above helpers, no section-divider comments. Third parties never appear in unit tests
(`WebKeysConfiguration`, `AppConfiguration`, `ConfigurationFactory`, `AbstractCryptoProvider`,
`CommonUtils` are mocks or `mockStatic`).

| Test class | Covers |
|------------|--------|
| `AccessTokenTypeTest` (model) | `fromString` variants; unknown → `null`; wire values |
| `ClientTest` (common, extend) | precedence matrix of `getResolvedAccessTokenType()` |
| `RegisterParamsValidatorTest` (extend) | every `JWT-SVID` constraint; type wins over `access_token_as_jwt`; `dpopBoundAccessToken` forced |
| `SpiffeOwnBundleServiceTest` | enc / HMAC excluded; `use` relabelled; no jans-specific members; `kty`/`alg` as strings; `spiffe_sequence` = revision; empty → `keys: []` |
| `SpiffeBundleRestWebServiceImplTest` | flag disabled; headers |
| `SpiffeTrustDomainServiceTest` | pin on first start; adopt another node's value; invalid domain → `ERROR`, no exception |
| `SpiffeIdUtilTest` (extend) | `buildAgentSpiffeId`; trust domain with `/` rejected |
| `AttestationServiceTest` | challenge TTL and single use; `key_mismatch`; `nonce_mismatch`; `client_id` ↔ key ownership; `warning` only with the flag; `latestAffirming` picks newest unexpired |
| `AttestationResultTokenServiceTest` | claims, `typ`, verification path |
| `AttestationRestWebServiceImplTest` | flag disabled; `invalid_challenge` carries a fresh challenge; no `500` for a verdict |
| `ExternalAttestationServiceTest` | selection by `evidence_format`; `false` → rejected; result copied |
| `RegisterValidatorTest` (extend) | `validateAttestation` by id / JWT; expired; wrong purpose; key not in `jwks`; `stale_evidence` has `challenge_id` |
| `TokenRestWebServiceValidatorTest` (extend) | grant type; flag; `invalid_target`; freshness rule none / stale / fresh; inline paths; `attestation_required` body |
| `DpopServiceTest` (extend) | `validateDpopKeyIsRegistered`: match, mismatch, no JWKS, `jwks_uri` |
| `JwtSvidBuilderTest` | `sub` derivation; `aud` = resources only; `cnf.jkt`; `txn` sources; `attestation`; `act` present / absent; HMAC rejected; `iss`, `jti`, `exp`, `nbf` |
| `AbstractAuthorizationGrantTest` (extend) | SVID lifetime source, cap, key-regeneration interaction |
| `IntrospectionWebServiceTest` (extend) | SPIFFE `sub`, `cnf`, `txn`, `attestation`, `act` |
| `IdJagServiceTest` (extend) | `txn` minted; continuation only with the flag |
| `TraceRecordBuilderTest` | envelope; canonical bytes stable; signature verifies; no token material in `tokens[]`; `bundle_hash` changes with policy config |
| `TraceChainStateTest` | monotonic sequence; `prev_record_hash` links; zero hash first |
| `TraceEmitterTest` | outbox retry; inline ordering |

Integration tests (`jans-auth-server/client`, disabled by default in `testng.xml` like the SPIFFE
client-auth suite, enabled in the CI profile that provisions the prerequisites): the hardware is
the static test keystore already used by the DPoP tests (public keys hosted at `clientJwksUri`);
the verifier is the same reference script configured with `trusted_verifier_jwks_uri =
clientJwksUri`, the test signing the attestation result itself; SPIRE is replaced by a
**self-federation round trip** (`spiffeTrustDomains` lists the server's own trust domain and
bundle URL, so an SVID issued to agent A is accepted by the shipped inbound validator as a
`jwt-spiffe` `client_assertion` for client B); the SPIFFE-native relying party is the test itself
verifying against the fetched bundle. Classes: `SpiffeBundleEndpointHttpTest`,
`AccessTokenTypeRegistrationHttpTest`, `AttestationHttpTest`, `AttestedRegistrationHttpTest`,
`JwtSvidIssuanceHttpTest` (including `attestation_required`, attest-and-issue, introspection),
`JwtSvidRoundTripHttpTest`, `TraceEmissionHttpTest` (against a Lock instance in the CI profile).
Nothing in the suite calls a host the project does not control.

## 15. Failure modes

| Event | Outcome |
|-------|---------|
| JWT-SVID stolen in transit or from a log | Replay fails at any `cnf`-checking verifier: no DPoP proof without the hardware key. At bearer-only verifiers it works until `exp`, which is why the TTL is minutes and high-value RPs should check `cnf` |
| Agent host compromised, key exfiltration attempted | Key is non-exportable; the attacker can only ask the hardware to sign while resident. Proof freshness windows and short SVID TTLs bound the abuse; the next re-attestation (`debug_enabled`, measurement drift) or disabling the client ends it |
| Evidence replayed from an earlier session | Challenge is single-use and bound to `jkt`; `nonce_mismatch` → `contraindicated`; the negative appraisal is itself recorded |
| Evidence about a different key than the DPoP / registration key | `attested_jkt` compared to the challenge `jkt` and to the registered keys in core; `key_mismatch`; the script cannot override |
| Platform drifts after enrollment | Re-attestation yields `contraindicated`; once the last affirming assessment ages past the maximum, issuance stops with `attestation_required`; outstanding SVIDs die within the TTL; introspection shows the stale `attestation` |
| Platform verifier unreachable | Agent cannot obtain a result; existing assessments stay valid until `expires_at`; issuance continues inside the window, then stops. Lengthening the window is visible as a new `bundle_hash` |
| Agent decommissioned or compromised | Admin disables the client: issuance stops; tokens die within the TTL; introspection reports `active: false` |
| Bundle endpoint unreachable | Verifiers keep their cached bundle (SPIRE natively; jans's own `SpiffeBundleService` serves stale with negative cache). Degrades only after cached keys rotate out |
| Signing key rotated | Bundle serves old + new for an overlap window and bumps `spiffe_sequence`; only an emergency rotation that drops the old key immediately invalidates in-flight tokens, by design |
| Client registers its own SPIFFE ID or a foreign trust domain | Impossible by construction: `sub` is derived from `client_id` under jans's own trust domain; the inbound `spiffe_id` attribute plays no role in issuance |
| jans-lock unreachable | Records queue in the outbox; auth continues. In `inline` mode the request fails, which is correct for a demo of accountability |
| TRACE producer key compromised | Revoke via Lock's admin API; later records rejected; rotate the alias |
| Attestation script accepts everything | Core still enforces nonce, key binding, challenge lifecycle and `status` semantics; a script cannot mint `affirming` for a key the evidence does not name |
| ID-JAG stolen | Audience- and client-bound, short-lived (existing); the SVID carries `act` and the records show which agent used it |
| Auditor doubts jans itself | Every claim is a signed record in a hash chain with Lock receipts; the appraisal is traceable to the platform verifier's own signature (`verifier`, `evidence_digest`), and with `attestationStoreEvidence=true` the raw evidence can be re-verified |

## 16. Specs to follow

| Spec | Role | Status in jans |
|------|------|----------------|
| SPIFFE JWT-SVID, Trust Domain & Bundle, Federation (`https_web`) | Token format; bundle format; how SPIRE federates with us | Validated / consumed inbound; **new**: issue and serve |
| RFC 9449 DPoP | Proof of possession at issuance and presentation | **Implemented** |
| RFC 7800 `cnf`, RFC 7638 thumbprint | Key binding | Thin layer over DPoP |
| RFC 8707 Resource Indicators | `resource` carries the SVID audience | New usage |
| RFC 9711 EAT, RFC 9334 RATS, draft-ietf-rats-ar4si (EAR) | Evidence envelope, roles, `status` vocabulary | **New** (attestation endpoint) |
| draft-ietf-oauth-attestation-based-client-auth | Where IETF is heading for presenting attestation to an AS; align the `attestation` parameter and result-token shape as it stabilises; also the WIT-SVID prerequisite | Track |
| draft-ietf-wimse-s2s-protocol (WIT) | A JWT-SVID with `cnf` is functionally a WIT; WIT may mandate its own `typ` | Track; issue plain JWT-SVID + additive claims now |
| draft-ietf-oauth-identity-assertion-authz-grant (ID-JAG) | Human → agent delegation | **Implemented**; `txn` added |
| RFC 8693 §4.1 `act`, RFC 8417 §2.2 `txn` | Delegation and correlation claims | New usage |
| RFC 7662 Introspection | Online verification | **Implemented**; new claims |
| OpenID AuthZEN 1.0 | Decision API the PEP calls | **Implemented** |
| TRACE v0.2 (agentrust) | Record semantics, `references` registry, RFC 8785 canonicalisation, Ed25519 | Lock profile exists; export mapping |
| OWASP GovOps | Capability catalogue naming (`capability_id` values) | Align as published |

## 17. Resolved decisions and open questions

### Resolved

- **No new `grant_type`.** Token format is orthogonal to authorization basis.
- **Format is a client property**, `access_token_type=JWT-SVID`; `access_token_as_jwt` deprecated.
- **Bundle keys = OIDC signing keys** for v1, relabelled in the bundle output; dedicated key set is
  a follow-up.
- **`accessTokenType` and `attestation` live in `ClientAttributes`** (`jansAttrs`), no client
  schema change.
- **Attestation is an endpoint + script**, not a DCR script alone; DCR and `/token` call the same
  service.
- **Delegate, don't parse**: v1 verifies a platform verifier's signed result; TPM quotes later.
- **Trust domain is pinned** on first start and never derived again.
- **Update-token script may add but not remove** the SVID's identity, binding and correlation
  claims.

### Open

- **Attestation freshness defaults.** `attestationMaxAgeForIssuanceSeconds=300`,
  `attestationRequireFreshPerIssuance=false`. Per-issuance attestation doubles token-endpoint
  latency on Azure Attestation; decide per capability class via the client attribute.
- **`txn` continuation.** Reusing an execution id lets a client collapse unrelated runs into one
  chain. Restrict to flagged clients and bound an execution's lifetime
  (`traceExecutionMaxAgeSeconds`).
- **Who mints `txn` without a human?** Pure `client_credentials` agents get a fresh `txn` per
  SVID. Confirm, or let the first PDP decision open the execution.
- **`act` direction.** RFC 8693 `act` names the actor acting on behalf of `sub`; here `sub` is the
  agent and the human delegates, closer to `may_act`. Confirm the claim name before PR 5; content
  does not change.
- **`resource` multiplicity.** RFC 8707 allows several; `aud` then becomes an array. Restrict to
  one value in v1 unless relying parties confirm array `aud` is fine.
- **Discovery member name.** `spiffe_bundle_endpoint`; confirm against SPIFFE Federation
  terminology before shipping (renaming later breaks consumers).
- **SPIFFE ID path shape.** `spiffe://<td>/agent/<client_id>`. If operators need
  human-meaningful paths, allow an admin-approved segment while keeping the `client_id` suffix
  mandatory.
- **Lock event kind for attestation.** New kind preferred; fallback flag keeps PR 6 independent.
- **Storing raw evidence.** SEV-SNP / TDX reports can contain platform identifiers; default off,
  digest always stored.
- **Revocation story.** Short TTL + disable client + introspection. Confirm acceptable before
  promising anything stronger.

## 18. What this plan deliberately excludes

- **X.509-SVID issuance / a CA.** JWT-SVIDs cover the agent use case. Revisit only with a concrete
  mTLS-between-agents requirement.
- **The SPIFFE Workload API.** Agents fetch tokens over HTTPS from `/token`; we do not replace
  SPIRE Agent.
- **A jans-hosted verifier for raw SEV-SNP / TDX / Nitro evidence.** The script verifies a
  platform verifier's result; TPM 2.0 quotes are the one in-process exception, after v1.
- **Coupling to agentrust-io.** We target the IETF standards it profiles (EAT / RATS) and jans-lock's
  TRACE profile; the agentrust record is an export (11.4). An agent manifest could later be one
  accepted *input* at enrollment, never a protocol dependency.
- **Node / workload attestation plugins à la SPIRE.** EAT plus platform verifiers is the single
  envelope.
- **Changes to PDP / PEP record formats** beyond copying `txn`, token `jti`s and `capability_id`.
- **SCITT transparency logging.** Lock's receipt chain is the anchor for now; a receipt URI can be
  added to records later without changing producers.
