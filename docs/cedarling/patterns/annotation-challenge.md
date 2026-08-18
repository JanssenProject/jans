---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Pattern: Challenges and Step-Up

!!! note "Illustrative example"

    Acme, ReportBot, and every annotation key below (`@challenge`, `@challenge_ttl_seconds`,
    `@containment`, and the rest) are invented for this page. Cedarling attaches no meaning to
    them; they work only because the example application reads them. The scenario is simplified to
    show the mechanism, not to be copied as a security design. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme runs an internal assistant, `ReportBot`, that employees use to answer questions over company
documents. It reads the same document store the employees can read, on their behalf.

Dana, who works in the Northeast financial-services team, asks ReportBot a broad question, and to
answer it the assistant reaches for `Acme::Document::"strategy-fy27"`, a company-level strategy
document classified `highly_confidential`, normally read only by the strategy and finance teams.

Nothing here is obviously an attack. There are three plausible explanations:

- Dana genuinely needs the document and nobody told the access system.
- The assistant over-reached and expanded a vague question into a search Dana never intended, so a
  document Dana never asked for was opened under Dana's identity.
- Dana's credentials are being used by somebody else.

A binary access system has to pick one. Denying blocks legitimate work and trains people to route
around the tool. Allowing means an agent exfiltrates strategy documents at machine speed, and the
security team finds out from a log review next week.

Neither has to be the answer. The system can ask instead: confirm with the human that the access
was intended, and get the data owner to confirm the need.
[Beyond Zero](https://queue.acm.org/detail.cfm?id=3819083) calls this a *challenge*, friction
applied in the moment and in proportion to the risk, instead of a flat denial.

## Modeling the Agent

The policies below are written against the identity model, so the identity model comes first.

The agent is a principal in its own right, not a flag on the human. `ReportBot` has its own entity,
its own ID, and its own attributes, and it carries a reference to the human who operates it:

- `Acme::Agent::"reportbot-7f3"` is the accessor that made the request
- `operated_by: Acme::User::"dana"` is the human accountable for it

Two consequences follow. Policies tell "an agent did this" from "a human did this" by the principal
type, which is part of the policy scope and therefore enforced by the engine, rather than by reading
a string out of the context that nothing prevents the caller from setting. And the reference to the
operator keeps the request attributable to a person even when no person typed it. Lose that
reference and the agent has ambient authority nobody owns.

An agent-initiated request and a human-initiated request are therefore two different requests, with
different principals, evaluated against different (though overlapping) policies.

## Why Annotations Are the Right Place for the Remedy

The decision is still binary. Cedar has `permit` and `forbid`, and adding a third outcome would
mean forking the policy language. `Challenge` is not a decision but a status attached to a
denial, saying "denied for now, and here is what would change the answer."

An annotation carries that well:

- The policy that denied the request knows *why*, so it is the right place to record *what would
  resolve it*. The condition and the remedy stay in one reviewable unit.
- The set of challenges is a policy decision, not an application decision. Which risky combinations
  deserve a security-key touch and which deserve owner approval is a question for the security
  team, and it changes far more often than the application code.
- The application only needs the *names* of the challenges it can run. It never needs to know under
  which conditions each one applies.

## The Flow

```
 ┌──────┐        ┌─────┐            ┌───────────┐      ┌───────────────────┐
 │ Dana │        │ App │            │ Cedarling │      │ Challenge service │
 └──┬───┘        └──┬──┘            └─────┬─────┘      └─────────┬─────────┘
    │  prompt       │                     │                      │
    │──────────────▶│                     │                      │
    │               │  1. authorize_unsigned                      │
    │               │  principal: Agent::"reportbot-7f3"          │
    │               │  completed_challenges: []                   │
    │               │────────────────────▶│                      │
    │               │  Deny, reason: [    │                      │
    │               │    forbid_agent_read_sensitive_without_intent,
    │               │    forbid_offscope_sensitive_read_by_agent ]│
    │               │◀────────────────────│                      │
    │               │                     │                      │
    │               │  2. annotation_values(reason, "challenge")  │
    │               │────────────────────▶│                      │
    │               │  ["confirm_human_intent",                  │
    │               │   "data_owner_approval"]                   │
    │               │◀────────────────────│                      │
    │               │                     │                      │
    │               │  3. run both challenges, each to the actor its policy names
    │               │───────────────────────────────────────────▶│
    │  "ReportBot wants to open strategy-fy27. Did you ask for this?"
    │◀──────────────────────────────────────────────────────────│
    │  confirm      │                     │                      │
    │──────────────────────────────────────────────────────────▶│
    │               │  attested results (server-side, TTL-bound)  │
    │               │◀───────────────────────────────────────────│
    │               │                     │                      │
    │               │  4. re-authorize, same principal            │
    │               │  completed_challenges: [both]               │
    │               │────────────────────▶│                      │
    │               │  Allow, reason: [permit_read_documents]     │
    │               │◀────────────────────│                      │
    │  answer       │                     │                      │
    │◀──────────────│                     │                      │
```

Step 4 is the important one. The challenge results go back in as context, and the same policies are
evaluated again. No policy is bypassed and no verdict is overridden. The request became a different,
better-evidenced request.

The principal is the agent, and an agent cannot confirm intent or touch a security key, so both
prompt for either of those goes to `principal.operated_by`.

Owner approval is the opposite case. Routing it to the operator would mean the operator approves
their own off-scope read, and the control the policy was written for turns into a click. It goes to
whoever owns the document, which the resource already records in `owner_team`.

Routing is the application's job, and `@challenge_actor` is how each policy says where its prompt
belongs: `controlling_human` for the two the operator answers, `data_owner` for the one they must
not.

## Schema

Both principal types apply to the action, and the challenge results are part of the context so the
policies can test them.

```cedarschema
namespace Acme {
    entity User = {
        "assignments": Set<String>,
        "risk_tier": String,
    };

    entity Agent = {
        "operated_by": User,
        "model": String,
    };

    entity Document = {
        "sensitivity": String,
        "subject": String,
        "owner_team": String,
    };

    action "Read" appliesTo {
        principal: [User, Agent],
        resource: [Document],
        context: {
            "completed_challenges": Set<String>,
        }
    };
}
```

## Policies

One broad `permit`, with the risk conditions layered on top as annotated `forbid` policies:

```cedar
@id("permit_read_documents")
permit (
    principal,
    action == Acme::Action::"Read",
    resource is Acme::Document
);
```

An agent reading a highly confidential document must have the human confirm that this is what they
asked for. This condition is about the *nature of the accessor*, so it exists only for `Agent`:

```cedar
@id("forbid_agent_read_sensitive_without_intent")
@challenge("confirm_human_intent")
@challenge_actor("controlling_human")
@challenge_ttl_seconds("300")
@user_message_id("access.challenge.confirm_human_intent")
forbid (
    principal is Acme::Agent,
    action == Acme::Action::"Read",
    resource is Acme::Document
)
when { resource.sensitivity == "highly_confidential" }
unless {
    context.completed_challenges.contains("confirm_human_intent")
};
```

Reading a highly confidential document outside your work assignment needs the data owner's
approval. This one is about the *human's scope of work*, and it applies whether the human reads
the document directly or an agent reads it for them. So it exists twice, once per principal type,
differing only in how it reaches the human's assignments:

```cedar
@id("forbid_offscope_sensitive_read_by_agent")
@challenge("data_owner_approval")
@challenge_actor("data_owner")
@challenge_strength("high")
@challenge_ttl_seconds("3600")
@user_message_id("access.challenge.owner_approval")
forbid (
    principal is Acme::Agent,
    action == Acme::Action::"Read",
    resource is Acme::Document
)
when {
    resource.sensitivity == "highly_confidential" &&
    !principal.operated_by.assignments.contains(resource.subject)
}
unless {
    context.completed_challenges.contains("data_owner_approval")
};
```

```cedar
@id("forbid_offscope_sensitive_read_by_user")
@challenge("data_owner_approval")
@challenge_actor("data_owner")
@challenge_strength("high")
@challenge_ttl_seconds("3600")
@user_message_id("access.challenge.owner_approval")
forbid (
    principal is Acme::User,
    action == Acme::Action::"Read",
    resource is Acme::Document
)
when {
    resource.sensitivity == "highly_confidential" &&
    !principal.assignments.contains(resource.subject)
}
unless {
    context.completed_challenges.contains("data_owner_approval")
};
```

Evaluated against Dana's request, the agent principal trips both agent policies:

```text
DENY   reason: forbid_agent_read_sensitive_without_intent
               forbid_offscope_sensitive_read_by_agent
```

Had Dana opened the document directly, only the scope rule would have fired: one challenge instead
of two, which is the proportionality the design is after.

```text
DENY   reason: forbid_offscope_sensitive_read_by_user
```

And once both challenges are recorded in the context, the same policy set allows the request:

```text
ALLOW  reason: permit_read_documents
```

### Notes on the Shape of This

Each challenge is its own policy. Cedar rejects a duplicate annotation key on one policy, so
`@challenge` appears once per policy. The constraint has a useful consequence: independent risk
conditions stay independent policies, they are reviewed separately, and the ones that matched are
the ones reported in `reason()`.

Each `forbid` clears itself through `unless`. A `forbid` with no way to clear it is a hard block,
not a challenge. The `unless` clause makes the denial resolvable, and it is why the second
authorization call can legitimately return `Allow`.

For a resolvable denial, the guidance lives on `forbid` rather than on `permit`. How decisions are
reported forces this: on a `Deny`, only the satisfied `forbid` policies appear in `reason()`, and if
nothing matched at all, `reason()` is empty and there are no annotations to read. Annotations on
`permit` policies are not wasted, they simply speak on the allow path, which is what the [quota
page](./annotation-quota.md) is built on. A broad `permit` with annotated `forbid` layers on top
keeps hints available at the moment they are needed. See [Only the determining policies are
reported](./annotation-patterns.md#only-the-determining-policies-are-reported).

One rule, two principal types, two policies. Cedar's scope cannot match a union of principal types,
so a rule covering both is either written twice or written once with the type test moved into the
`when` clause, narrowing on `principal is Acme::User` and `principal is Acme::Agent` in two branches
of an `||`. That single-policy form validates. The pair is still worth the duplication here: each
principal type gets its own `@id`, so `reason()` and the decision log say which shape fired, and a
reviewer reads one condition at a time. Keeping the `@challenge` value identical across the pair
means the application still sees one challenge, whichever policy fired. If the duplication grows
past a handful of pairs, mirror the operator's decision-relevant attributes onto the agent entity
when it is built (`agent.assignments` copied from `operated_by`). That collapses each pair into a
single policy at the cost of a denormalized entity. It is a trade between policy-store duplication
and entity-builder complexity, and it is worth deciding once for the whole policy set rather than
per policy.

## Application Code

```rust
use std::collections::BTreeSet;

// Server-issued records only, already filtered to this action and resource and to
// records whose @challenge_ttl_seconds has not expired. Never the raw client context.
let mut completed: BTreeSet<String> = session.valid_challenges_for(&document, Action::Read);

for attempt in 0..2 {
    let result = cedarling
        .authorize_unsigned(read_request(&agent, &document, &completed))
        .await?;

    if result.decision {
        return Ok(serve(result));
    }

    let reason: Vec<_> = result.response.diagnostics().reason().collect();

    // Every challenge asked for by a policy that actually matched.
    let asked = cedarling.annotation_values(reason.iter().copied(), "challenge");

    // No guidance, or the challenges were already attempted: a plain denial.
    if asked.is_empty() || attempt == 1 {
        let message_id = cedarling
            .annotation_values(reason.iter().copied(), "user_message_id")
            .into_iter()
            .next()
            .unwrap_or_else(|| "access.denied.generic".to_string());
        return Err(deny(message_id));
    }

    // Each policy's annotations stay grouped: the challenge it asked for, the TTL that
    // applies to it, and the message that belongs with it. This is also the audit trail.
    let by_policy = cedarling.annotations_by_policy(reason.iter().copied());
    audit.record(&by_policy);

    // Run the challenges. Results are attested server-side, never taken from the client.
    for name in asked {
        // Who answers is the policy's call, not this loop's: an agent cannot confirm
        // its operator's intent, and an operator cannot approve their own off-scope read.
        let responder = match actor_of(&by_policy, &name) {
            "data_owner" => Responder::Team(&document.owner_team),
            _ => Responder::User(&agent.operated_by),
        };

        match challenge_service
            .run(&name, responder, strength_of(&by_policy, &name), &document)
            .await?
        {
            Outcome::Passed => {
                // Stored server-side, scoped to this resource and action, and stamped
                // with the @challenge_ttl_seconds of the policy that asked for it.
                session.record_challenge(&name, &document, Action::Read, ttl_of(&by_policy, &name))?;
                completed.insert(name);
            }
            Outcome::Failed | Outcome::Abandoned => return Err(deny_and_escalate(name)),
        }
    }
}

/// Every group whose `challenge` is `name`. `by_policy` is keyed by policy ID, so
/// the challenge name has to be looked up in the values.
fn groups_for<'a>(
    by_policy: &'a HashMap<String, HashMap<String, String>>,
    name: &str,
) -> impl Iterator<Item = &'a HashMap<String, String>> {
    let name = name.to_string();
    by_policy
        .values()
        .filter(move |a| a.get("challenge") == Some(&name))
}

/// The shortest TTL any matching policy asked for. Two policies can name the same
/// challenge, `reason()` has no order, so taking the first one found would pick
/// nondeterministically. A policy that set no TTL gets the application's floor.
fn ttl_of(by_policy: &HashMap<String, HashMap<String, String>>, name: &str) -> u64 {
    groups_for(by_policy, name)
        .map(|a| {
            a.get("challenge_ttl_seconds")
                .and_then(|t| t.parse().ok())
                .unwrap_or(MIN_CHALLENGE_TTL_SECONDS)
        })
        .min()
        .unwrap_or(MIN_CHALLENGE_TTL_SECONDS)
}

/// Who has to answer, and how hard the challenge should be. Both default to the
/// strictest reading when a policy says nothing, and disagreement resolves the same
/// way: two policies naming different actors for one challenge is a policy bug, and
/// picking whichever the iterator reached first would hide it behind a coin flip.
fn actor_of<'a>(by_policy: &'a HashMap<String, HashMap<String, String>>, name: &str) -> &'a str {
    let actors: BTreeSet<&str> = groups_for(by_policy, name)
        .filter_map(|a| a.get("challenge_actor"))
        .map(String::as_str)
        .collect();

    match actors.len() {
        1 => actors.iter().next().copied().unwrap(),
        _ => "controlling_human",
    }
}

fn strength_of(by_policy: &HashMap<String, HashMap<String, String>>, name: &str) -> Strength {
    groups_for(by_policy, name)
        .filter_map(|a| a.get("challenge_strength"))
        .map(|s| Strength::parse(s))
        .max()
        .unwrap_or(Strength::High)
}
```

```javascript
const result = await cedarling.authorize_unsigned(JSON.stringify(request));

if (!result.decision) {
  const reason = result.response.diagnostics.reason;

  // Grouped, so each challenge keeps its own TTL and message.
  const asked = Object.values(cedarling.annotations_by_policy(reason))
    .filter((a) => a.challenge);

  if (asked.length === 0) {
    return denyPlain(
      cedarling.annotations_map(reason).user_message_id ?? "access.denied.generic",
    );
  }
  return requestChallenges(asked);
}
```

Nothing in that loop trusts the caller. The set fed into `context.completed_challenges` is rebuilt
from server-side records that are scoped to this resource and action and dropped once their TTL has
passed, which is what makes the policy's `unless` clause mean anything. The requirements below spell
out why each of those properties matters.

The retry is bounded on purpose. One re-evaluation after the challenges pass is enough. Looping
until `Allow` turns a policy misconfiguration into an infinite challenge prompt.

## Security Requirements

The pattern is only as strong as the integrity of `context.completed_challenges`, so treat these as
part of the implementation rather than as advice.

- Never accept completed challenges from the client. A challenge result has to be produced by the
  challenge service and held in server-side session state, or in a signed, audience-bound token. If
  a browser or an agent can put `"security_key_touch"` into the context, the policy enforces
  nothing.
- Enforce the TTL. `@challenge_ttl_seconds` is a hint the application has to act on: drop the
  completed challenge from the context once it expires, so long-lived sessions cannot ride one touch
  forever. Cedar has no concept of elapsed time here, so nothing enforces it if the application does
  not. Treat a policy that carries no TTL as the shortest one you support rather than as unbounded.
  Otherwise the one challenge somebody forgot to annotate is the one that never expires.
- Bind the result to the request. A challenge passed for one document should not silently authorize
  a bulk export of a thousand others. Scope the stored result to the action and resource it was
  raised for, or to a short window, whichever your risk model justifies.
- Address the challenge to someone who can refuse it. A challenge an agent satisfies on its own is
  not a challenge, and one the requester answers about their own request is not either.
  `@challenge_actor` names the party per policy: `controlling_human` resolves to
  `principal.operated_by`, `data_owner` to the team in `resource.owner_team`. Record who answered,
  and reject an answer from the principal the challenge exists to constrain.
- Log the failures. A challenge that is repeatedly abandoned is a signal in itself, and it belongs
  in the same stream as the decision logs.

Because those records live on the server, a browser evaluating these same policies is working from a
copy it cannot refresh, so its denials are provisional. See
[Advisory denials in a client-side PDP](./annotation-client-authority.md).

## Variation: Risk Carried by the Operator

Risk attaches to the human, but the request arrives from the agent. Reaching through `operated_by`
keeps the two connected, so an elevated-risk operator does not get a quieter path by sending an
assistant:

```cedar
@id("forbid_agent_read_for_elevated_risk_operator")
@challenge("security_key_touch")
@challenge_actor("controlling_human")
@challenge_ttl_seconds("60")
@user_message_id("access.challenge.security_key")
forbid (
    principal is Acme::Agent,
    action == Acme::Action::"Read",
    resource is Acme::Document
)
when {
    principal.operated_by.risk_tier == "elevated" &&
    resource.sensitivity == "highly_confidential"
}
unless {
    context.completed_challenges.contains("security_key_touch")
};
```

## Variation: Containment

A challenge is resolvable in the moment. Some outcomes should not be. After several failed
challenges, or when an investigation has already flagged the account, the right answer is a durable
block that a person has to lift.

That is the same annotation mechanism with the `unless` clause removed, plus a different vocabulary
so the application does not offer a way out that does not exist:

```cedar
@id("forbid_contained_operator")
@containment("session_freeze")
@user_message_id("access.contained.contact_security")
@escalate_to("secops-oncall")
forbid (
    principal is Acme::Agent,
    action,
    resource
)
when { principal.operated_by.risk_tier == "contained" };
```

The human's own sessions need the twin, or containment stops an assistant and lets the operator
carry on by hand:

```cedar
@id("forbid_contained_user")
@containment("session_freeze")
@user_message_id("access.contained.contact_security")
@escalate_to("secops-oncall")
forbid (
    principal is Acme::User,
    action,
    resource
)
when { principal.risk_tier == "contained" };
```

The application reads `containment` rather than `challenge`, shows the message, and does not prompt:
there is no inline path from here back to `Allow`. Lifting it means changing the `risk_tier`, which
happens outside the request.

Containment depends on an attribute being present, and it is the one policy here that must never
stop applying unnoticed. When the policy store carries a schema, Cedarling validates entities
against it, so an operator entity built without `risk_tier` fails the request instead of slipping
past the containment policy. Without a schema there is nothing to check: the policy errors at
evaluation, drops out of the decision, and the denial it should have produced never happens. Ship a
schema, and keep `risk_tier` required in it rather than optional.

Containment is keyed on the human. Freezing one misbehaving agent while its operator keeps working
would need `risk_tier` on `Agent` and a third policy; this example leaves that out.

## What This Buys You

The application ends up with no hardcoded knowledge of *when* to challenge. It knows how to run
`confirm_human_intent`, `data_owner_approval`, and `security_key_touch`, it knows to address them to
the operator, and it knows how to read annotations. Security engineers add a condition, retire one,
or raise a challenge's strength by editing policies, and each of those changes is a diff you can
review, test against recorded requests, and find later in the decision log.
