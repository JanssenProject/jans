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

    Acme, ReportBot, and every annotation key below (`@challenge`, `@challenge_ttl`,
    `@containment`, and the rest) are invented for this page. Cedarling attaches no meaning to
    them; they work only because the example application reads them. The scenario is simplified to
    show the mechanism, not to be copied as a security design. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme runs an internal assistant, `ReportBot`, that employees use to answer questions over company
documents. It reads the same document store the employees can read, on their behalf.

Dana works in the Northeast financial-services team. She asks ReportBot a broad question, and to
answer it the assistant reaches for `Acme::Document::"strategy-fy27"`, a company-level strategy
document classified `highly_confidential`, normally read only by the strategy and finance teams.

Nothing here is obviously an attack. There are three plausible explanations:

- Dana genuinely needs the document and nobody told the access system.
- The assistant over-reached: it expanded a vague question into a search Dana never intended.
  Dana never asked for that document and would be surprised to learn it was opened in her name.
- Dana's credentials are being used by somebody else.

A binary access system has to pick one. Denying blocks legitimate work and trains people to route
around the tool. Allowing means an agent quietly exfiltrates strategy documents at machine speed,
and the security team finds out from a log review next week.

The third option is to ask: confirm with the human that the access was intended, and get the data
owner to confirm the need. [Beyond Zero](https://queue.acm.org/detail.cfm?id=3819083) calls this a
*challenge*, friction applied in the moment and in proportion to the risk, instead of a flat
denial.

## Modeling the Agent

The identity model has to be right before the policies make sense, because the policies are
written against it.

The agent is a principal in its own right, not a flag on the human. `ReportBot` has its own entity,
its own ID, and its own attributes, and it carries a reference to the human who operates it:

- `Acme::Agent::"reportbot-7f3"` is the accessor that made the request
- `operated_by: Acme::User::"dana"` is the human accountable for it

This matters for two reasons. The policies can tell "an agent did this" from "a human did this" by
looking at the principal type, which is what Cedar is good at, instead of reading a string out of
the context that nothing prevents the caller from setting. And losing the link to the operator is
how agents end up with ambient authority nobody owns: the request has to stay attributable to a
person even when no person typed it.

An agent-initiated request and a human-initiated request are therefore two different requests, with
different principals, evaluated against different (though overlapping) policies.

## Why Annotations Are the Right Place for the Remedy

The decision is still binary. Cedar has `permit` and `forbid`, and adding a third outcome would
mean forking the policy language. `Challenge` is not a decision. It is a status attached to a
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
    │               │  3. run both challenges, addressed to the operator
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
evaluated again. No policy is bypassed and no verdict is overridden. The request genuinely became a
different, better-evidenced request.

Note who the challenge is addressed to. The principal is the agent, but an agent cannot confirm
intent or touch a security key, so both prompts go to `principal.operated_by`. That routing is the
application's job, and the `@challenge_actor` annotation below states it instead of leaving it
implied.

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
@challenge_ttl("300")
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
@challenge_strength("high")
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
@challenge_strength("high")
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

Had Dana opened the document herself, only the scope rule would have fired: one challenge instead
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
`@challenge` appears once per policy. That constraint pushes the design somewhere useful:
independent risk conditions stay independent policies, they are reviewed separately, and the ones
that matched are the ones reported in `reason()`.

Each `forbid` clears itself through `unless`. A `forbid` whose condition can never be satisfied is a
hard block, not a challenge. The `unless` clause makes the denial resolvable, and it is why the
second authorization call can legitimately return `Allow`.

The guidance lives on `forbid`, not on `permit`. How decisions are reported forces this: on a
`Deny`, only the satisfied `forbid` policies appear in `reason()`, and if nothing permitted the
request at all, `reason()` is empty and there are no annotations to read. A broad `permit` with
annotated `forbid` layers on top keeps hints available at the moment they are needed. See
[Only the determining policies are reported](./annotation-patterns.md#only-the-determining-policies-are-reported).

One rule, two principal types, two policies. Cedar's scope cannot match a union of principal types,
and `User` and `Agent` share no attribute interface, so a rule that applies to both is written
twice. Keeping the `@challenge` value identical across the pair means the application still sees one
challenge, whichever policy fired. If the duplication grows past a handful of pairs, mirror the
operator's decision-relevant attributes onto the agent entity when it is built (`agent.assignments`
copied from `operated_by`). That collapses each pair into a single policy at the cost of a
denormalized entity. It is a trade between policy-store duplication and entity-builder complexity,
and it is worth deciding once for the whole policy set rather than per policy.

## Application Code

```rust
use std::collections::BTreeSet;

// Server-issued records only, already filtered to this action and resource and to
// records whose @challenge_ttl has not expired. Never the raw client context.
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
    // For an agent principal the prompt is addressed to `agent.operated_by`.
    for name in asked {
        match challenge_service.run(&name, &operator, &document).await? {
            Outcome::Passed => {
                // Stored server-side, scoped to this resource and action, and stamped
                // with the @challenge_ttl of the policy that asked for it.
                session.record_challenge(&name, &document, Action::Read, ttl_of(&by_policy, &name))?;
                completed.insert(name);
            }
            Outcome::Failed | Outcome::Abandoned => return Err(deny_and_escalate(name)),
        }
    }
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
    return denyPlain(cedarling.annotations_map(reason).user_message_id);
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
- Enforce the TTL. `@challenge_ttl` is a hint the application has to act on: drop the completed
  challenge from the context once it expires, so long-lived sessions cannot ride one touch forever.
  Cedar has no concept of elapsed time here, so nothing enforces it if the application does not.
- Bind the result to the request. A challenge passed for one document should not silently authorize
  a bulk export of a thousand others. Scope the stored result to the action and resource it was
  raised for, or to a short window, whichever your risk model justifies.
- Address the challenge to a human. A challenge that an agent can satisfy on its own is not a
  challenge. `@challenge_actor("controlling_human")` states the requirement, and the application has
  to route the prompt to `principal.operated_by` and record who answered.
- Log the failures. A challenge that is repeatedly abandoned is a signal in itself, and it belongs
  in the same stream as the decision logs.

## Variation: Risk Carried by the Operator

Risk attaches to the human, but the request arrives from the agent. Reaching through `operated_by`
keeps the two connected, so an elevated-risk operator does not get a quieter path by sending an
assistant:

```cedar
@id("forbid_agent_read_for_elevated_risk_operator")
@challenge("security_key_touch")
@challenge_actor("controlling_human")
@challenge_ttl("60")
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

The application reads `containment` rather than `challenge`, shows the message, and does not
prompt: there is no inline path from here back to `Allow`. Lifting it means changing the operator's
`risk_tier`, which happens outside the request. A twin policy on `principal is Acme::User` contains
the human's own sessions the same way.

## What This Buys You

The application ends up with no hardcoded knowledge of *when* to challenge. It knows how to run
`confirm_human_intent`, `data_owner_approval`, and `security_key_touch`, it knows to address them to
the operator, and it knows how to read annotations. Security engineers add a condition, retire one,
or raise a challenge's strength by editing policies, and each of those changes is a diff you can
review, test against recorded requests, and find later in the decision log.
