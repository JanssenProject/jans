---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Policy Annotation Patterns

A Cedar policy answers one question: is this action on this resource allowed, yes or no. That
answer alone is often not enough to build a product. The application also needs to know *what to
do next*: which message to show, which step-up challenge to run, where to send the user, whether
the response must be masked, or that the user is about to hit a quota.

Cedar annotations (`@key("value")`) let the policy author attach that guidance to the policy
itself, and Cedarling hands it back with the decision. Behavior that is normally hardcoded in the
application (wizard steps, escalation paths, warning thresholds) moves into the policy store,
where it can be reviewed, versioned, and changed without a code release.

This section collects patterns for doing that. Each page describes a scenario, the flow, the
policies, and the application code that reads the annotations.

The code samples assume you know how an authorization request carries its principal, resource, and
context. If that is new, read [Authorization](../reference/cedarling-authz.md) and
[Entities](../reference/cedarling-entities.md) first; nothing here changes how a request is built.

!!! note "These are illustrative examples, not a specification"

    Cedarling gives annotations no built-in meaning. `@challenge`, `@warn`, `@redirect_route`,
    `@user_message_id` and every other key on these pages were invented for the documentation:
    they are not reserved, not validated, and nothing in Cedarling reacts to them. An annotation
    only does something because *your* application reads that key and acts on it.

    The one exception is `@id`, which Cedarling uses as the policy ID.

    The same goes for the scenarios themselves. The companies, entities, schemas, and flows exist
    to show what the mechanism makes possible. They are deliberately simplified, they are not
    reference architectures, and they are not security advice for your system. Design your own
    vocabulary and your own policies around your own risk model, and treat these pages as a
    starting point for thinking rather than something to copy into production.

The pages build on each other, so read them in this order if the mechanism is new to you.

| Pattern | Problem it solves |
|---|---|
| [Quota and threshold warnings](./annotation-quota.md) | Warning the user before a limit turns into a denial |
| [Multi-step flows and escalation](./annotation-workflow.md) | Onboarding steps, redirects, approval routing, break-glass |
| [Challenges and step-up](./annotation-challenge.md) | A deny that says *how* to earn the allow: MFA, human confirmation, owner approval |
| [Shaping the response and mapping controls](./annotation-response-shaping.md) | Masking, row limits, and compliance metadata on an allow |
| [Advisory denials in a client-side PDP](./annotation-client-authority.md) | Telling a browser which of its own denials are worth rendering |

The first two read annotations as hints the user can act on, which is the forgiving case: ignoring
one costs you a worse experience and nothing more. Break-glass and response shaping treat them as
obligations the application has to enforce, which is a stricter contract, so read
[Obligations, not hints](./annotation-response-shaping.md#obligations-not-hints) before using that
shape. The last page is about consuming decisions rather than writing them, and it applies wherever
Cedarling runs somewhere its answer is provisional.

## The Mental Model

```text
                 ┌─────────────┐
  request ──────▶│  Cedarling  │───▶ decision (Allow / Deny)
                 │    (PDP)    │───▶ reason(): determining policy IDs
                 └─────────────┘             │
                                             ▼
                                 annotations_map / annotation_values
                                             │
                                             ▼
                        application (PEP) decides what to do with the hint
```

Three properties follow from this, and every pattern in this section respects them.

The decision stays binary. An annotation never changes `Allow` into `Deny` or the reverse.
`Challenge`, `warn`, and `mask` are statuses carried alongside a decision, not a third decision.

The policy names the intent and the application implements it.
`@challenge("security_key")` means the application knows a challenge called `security_key`. The
policy does not know how it is performed.

Annotations are static. The value is a literal string fixed at authoring time. It cannot
interpolate context, entity attributes, or the current counter value. Anything dynamic is resolved
by the application after it reads the hint.

## Mechanics You Need to Know

These constraints shape how the patterns are written, so read them before designing an annotation
vocabulary.

### Only the Determining Policies Are Reported

`result.response.diagnostics().reason()` contains the policies that determined the outcome, and
those are the only policies whose annotations you can resolve. Cedar reaches a decision in three
ways, and each one reports something different:

| Outcome | How it happened | `reason()` contains | Annotations you can read |
|---|---|---|---|
| `Allow` | at least one `permit` matched, no `forbid` did | the satisfied `permit` policies | the annotations on those `permit` policies |
| `Deny` | a `forbid` matched, whether or not a `permit` also did | the satisfied `forbid` policies | the annotations on those `forbid` policies |
| `Deny` | nothing matched at all, the default deny | nothing, `reason()` is empty | none |

The two `Deny` rows are different situations, not conflicting statements about one. In the first, a
policy author wrote a rule that actively blocked the request, and that rule can explain itself. In
the second, no rule blocked anything and no rule allowed anything either: Cedar denies by default,
and a default has no policy behind it to carry an annotation.

Note also that `permit` annotations never survive a `Deny`. When a `forbid` overrides a matching
`permit`, only the `forbid` appears in `reason()`, so a message you attached to the `permit` is not
available to explain the denial.

A fourth shape hides behind the third. A policy whose expression errors at evaluation time, on
integer overflow or on an attribute the entity does not carry, is dropped from the decision and
reported in `diagnostics().errors()` instead of `reason()`. If that policy was the only `permit`,
the result is a `Deny` with an empty `reason()`, indistinguishable from "nothing matched" to code
that reads only `reason()`. Log `errors()` alongside the decision, or a broken policy will look like
an ordinary default deny for as long as nobody checks.

The default-deny row is the one that catches people out. If you want a hint for a request that
simply matches no `permit`, an annotated `forbid` has to exist and match, as the quota example
does with its `forbid` at 100%. Otherwise the application needs its own fallback message for
"denied, no guidance available". Do not add a catch-all `forbid` just to hang an annotation on it:
in Cedar `forbid` always wins, so a broad catch-all silently overrides every `permit` you have.

### One Key per Policy, Several Values Means Several Policies

Cedar rejects a duplicate annotation key on the same policy (`duplicate annotation: @challenge`),
so a single policy carries at most one `@challenge`. Two challenges means two policies, each with
its own annotation, and both have to be satisfied for both to appear in `reason()`.

This works in your favor. Independent conditions stay independent policies, and
`annotation_values(reason, "challenge")` collects every value that actually fired, duplicates
preserved.

```rust
// Two forbid policies matched, each requesting a different challenge.
let challenges =
    cedarling.annotation_values(result.response.diagnostics().reason(), "challenge");
// ["confirm_human_intent", "data_owner_approval"]
```

Use `annotations_map` only when you know the keys cannot collide across policies, since it keeps
one arbitrary value per key. `annotations_by_policy` is the loss-free view, and it is the one you
want for audit logging because it records which policy asked for what.

### `reason()` Has No Order

`reason()` is a set of policy IDs, so the policies come back in no particular order, and the lookup
methods report values in whatever order they receive the IDs. Two consequences:

- Never pair two `annotation_values` results by position. The first value of one list does not
  necessarily come from the same policy as the first value of the other.
- If several annotations on one policy only mean something together, read them with
  `annotations_by_policy`, which keeps each policy's set intact.

Anything that has to appear in a specific order has to say so in an annotation, such as
`@wizard_step("2")`. That ordering key is only usable through `annotations_by_policy`, because it
has to stay attached to the values it orders and the lossy views drop that link. Read the groups,
then sort them in the application.

```rust
let by_policy = cedarling.annotations_by_policy(result.response.diagnostics().reason());

let mut steps: Vec<(u32, &str)> = Vec::new();

for (policy_id, a) in &by_policy {
    match (
        a.get("wizard_step").and_then(|s| s.parse::<u32>().ok()),
        a.get("required_step"),
    ) {
        (Some(step), Some(name)) => steps.push((step, name.as_str())),
        // A policy you cannot order is a bug to surface, not one to drop silently.
        _ => return Err(Error::MalformedAnnotation(policy_id.clone())),
    }
}

steps.sort();
```

Sorting the output of `annotation_values` would sort the values themselves, which is not the same
thing and is usually wrong.

### `@id` Is Reserved

Cedarling uses the `@id("...")` annotation as the policy ID. Keep it as the identifier and do not
overload it with other meaning. Every pattern here assumes stable, readable policy IDs, because
those IDs are what appear in `reason()` and in the decision logs.

### The Lookup Is Independent of the Authorization Method

Every pattern here is written against `authorize_unsigned`, but nothing in the mechanism depends on
it. `authorize_multi_issuer` reports the determining policies the same way, and the same three
lookup methods resolve their annotations. Only the principal and context construction differ.

### Resolve Annotations Promptly

Annotation lookup resolves IDs against the *current* policy store. A concurrent policy-store
refresh may swap the store between the authorization call and the lookup, in which case IDs that
no longer resolve are silently dropped. Resolve immediately after `authorize*()`, in the same
request handler.

## Use Message IDs, Not Message Text

You can write the user-facing string straight into the policy:

```cedar
@user_message("Your export needs approval from the data owner.")
```

That works, and it is fine for a prototype or a single-locale internal tool. For anything
user-facing, annotate a message ID instead and keep the text in your translation catalog:

```cedar
@user_message_id("access.export.owner_approval_required")
```

The message ID form keeps localization possible, lets the copy change without touching the policy
store, and lets one message be reused by several policies. It also keeps product copy, and its
review cycle, out of the authorization path.

The same reasoning applies to routes. Prefer a logical target the application can resolve
(`@redirect_route("kyc.documents")`) over a hardcoded path (`@redirect("/v2/kyc/documents")`), so
that a frontend restructure does not require a policy release.

## Anti-Patterns

Do not put secrets or personal data in annotations. Annotations travel with the policy store and
land in decision logs. They are policy metadata, readable by anyone who can read the policy.

Do not let an annotation override the decision. A PEP that lets a `Deny` proceed because the policy
carried `@enforcement("monitor")` has no enforcement at all. If you need a policy that observes
without blocking, express that in the policy logic instead of ignoring its verdict.

Do not encode structured data in one annotation. A JSON blob in a string is hard to review and easy
to break. Prefer several flat keys (`@quota_scope("monthly")`, `@quota_threshold("80")`) over
`@quota("{\"scope\":\"monthly\",\"threshold\":80}")`.

Do not invent keys per policy, and name the unit when a value has one. `@sla_hours`,
`@retention_days`, and `@challenge_ttl_seconds` cannot be misread; a bare `@ttl` invites somebody to
supply milliseconds. An annotation vocabulary is an interface between policy authors and application
developers. Keep a short, documented list of keys and their allowed values, and treat
adding a key as an API change: the application has to know how to handle it, and a key that no code
reads is a silent no-op.

Do not assume an annotation will be there. Policies change independently of the application, so
every lookup needs a sane default for "annotation absent".

## API Reference

The three lookup methods have the same names in Rust, Python, and JavaScript, and are documented in
the [Interfaces reference](../reference/cedarling-interfaces.md#annotation-lookup):

- `annotations_map(policy_ids)` returns a merged map, lossy on duplicate keys
- `annotation_values(policy_ids, key)` returns all values of one key, duplicates preserved
- `annotations_by_policy(policy_ids)` returns them grouped by policy ID, loss-free
