---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Pattern: Step-Up and Evidence

!!! note "Illustrative example"

    Acme, ReportBot, and every `@checks` value below are invented for this page. Cedarling attaches
    no meaning to them; they work only because the example application reads them. The scenario is
    simplified to show the mechanism, not to be copied as a security design. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme runs an internal assistant, `ReportBot`, that employees use to answer questions over company
documents. It reads the same document store the employees can read, on their behalf.

Dana, who works in the Northeast financial-services team, asks ReportBot a broad question, and to
answer it the assistant reaches for `Acme::Document::"strategy-fy27"`, a company-level strategy
document classified `highly_confidential`, normally read only by the strategy and finance teams.

Nothing here is obviously an attack. There are three plausible explanations:

- Dana genuinely needs the document and nobody told the access system.
- The assistant over-reached and expanded a vague question into a search Dana never intended.
- Dana's credentials are being used by somebody else.

A binary answer has to pick one. Denying blocks legitimate work and trains people to route around
the tool. Allowing means an agent reads strategy documents at machine speed, and the security team
finds out from a log review next week.

## Where the Challenge Is Decided

The better answer is to ask: confirm with Dana that the access was intended, get the data owner to
confirm the need, or require a security key. [Beyond Zero](https://queue.acm.org/detail.cfm?id=3819083)
calls this a *challenge*, friction applied in the moment and in proportion to the risk.

Choosing that challenge is not the policy's job. Which challenge to put in front of an accessor is
an organization-level decision that spans applications, and it is usually made by a reasoning engine
or an identity orchestration service. It weighs signals no single policy sees: the device, the
session's history, what the same account did in other applications an hour ago. A policy that named
its own challenge would be making that decision with a fraction of the information.

What the policy can do is say precisely what is missing. Its `@checks` label names the condition
that denied the request, and its `unless` clause names the evidence that would satisfy it. The
reasoning engine takes the label as one input among many and decides what to do: run a challenge,
refuse outright, or raise an alert.

Once a challenge is issued, the interaction becomes stateful. The accessor goes off to complete it,
the challenge service records the result, and the next request arrives with richer context. The
challenge itself carries no claims; it is the opportunity to collect them. The policy sees only the
outcome, as evidence in the context, and evaluates the request again.

## Modeling the Agent

The policies below are written against the identity model, so the identity model comes first.

The agent is a principal in its own right, not a flag on the human. `ReportBot` has its own entity
type, its own ID, and its own attributes, so policies tell "an agent did this" from "a human did
this" by the principal type, which the engine enforces, rather than by reading a string out of the
context that nothing prevents the caller from setting.

The agent also has to stay attributable to the human who operates it. Lose that link and the agent
has ambient authority nobody owns. An attribute of type `User` cannot express that link, because
[only the principal and the resource are
entities](./annotation-patterns.md#only-the-principal-and-the-resource-are-entities). So the agent
carries its operator as an identifier, together with copies of the operator attributes the policies
need, filled in by the server when it builds the request:

- `operator_id` keeps the request attributable to a person, and it is what the challenge service
  uses to reach the operator
- `operator_assignments` and `operator_risk_tier` are copies of the operator's own attributes, so
  rules about the human's scope and risk can be evaluated on an agent request

Make them required in the schema. A required attribute the server forgot to fill in fails the
request, while an optional one that is missing would make every policy reading it error, drop out of
the decision, and stop protecting anything.

## The Flow

```text
 Dana          App                  Cedarling        Reasoning engine     Challenge service
  |             |                       |                    |                    |
  |-- prompt -->|                       |                    |                    |
  |             |-- 1. authorize ------>|                    |                    |
  |             |   principal: Agent::"reportbot-7f3"         |                    |
  |             |   attestations: []    |                    |                    |
  |             |<-- Deny, reason: -----|                    |                    |
  |             |    forbid_agent_read_sensitive_unconfirmed  |                    |
  |             |    forbid_offscope_sensitive_read_by_agent  |                    |
  |             |                       |                    |                    |
  |             |-- 2. annotation_values(reason, "checks") -->|                    |
  |             |   agent.human_intent_unconfirmed            |                    |
  |             |   document.outside_operator_assignments     |                    |
  |             |                       |                    |                    |
  |             |-- 3. checks + signals ------------------->|                    |
  |             |<-- decision: run two challenges -----------|                    |
  |             |                       |                    |                    |
  |             |-- 4. run challenges ---------------------------------------->|
  |<-- "ReportBot wants to open strategy-fy27. Did you ask for this?" ---------|
  |-- confirm ---------------------------------------------------------------->|
  |             |                       |        (data owner approves too)    |
  |             |<-- attested results, server-side, bound to this request ---|
  |             |                       |                    |                    |
  |             |-- 5. re-authorize --->|                    |                    |
  |             |   attestations: [human_intent_confirmed, data_owner_approved]   |
  |             |<-- Allow -------------|                    |                    |
  |<-- answer --|                       |                    |                    |
```

Step 5 is the important one. The evidence goes back in as context, and the same policies are
evaluated again. No policy is bypassed and no verdict is overridden. The request became a different,
better-evidenced request.

## Schema

Both principal types apply to the action, and the collected evidence is part of the context so the
policies can test it.

```cedarschema
namespace Acme {
    entity User = {
        "assignments": Set<String>,
        "risk_tier": String,
    };

    entity Agent = {
        "operator_id": String,
        "operator_assignments": Set<String>,
        "operator_risk_tier": String,
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
            "attestations": Set<String>,
        }
    };
}
```

## Policies

One broad `permit`, with the risk conditions layered on top as labeled `forbid` policies:

```cedar
@id("permit_read_documents")
permit (
    principal,
    action == Acme::Action::"Read",
    resource is Acme::Document
);
```

An agent reading a highly confidential document needs evidence that the human intended it. This
condition is about the *nature of the accessor*, so it exists only for `Agent`:

```cedar
@id("forbid_agent_read_sensitive_unconfirmed")
@checks("agent.human_intent_unconfirmed")
@detail("An agent is reading a highly confidential document with no confirmation from its operator.")
forbid (
    principal is Acme::Agent,
    action == Acme::Action::"Read",
    resource is Acme::Document
)
when { resource.sensitivity == "highly_confidential" }
unless {
    context.attestations.contains("human_intent_confirmed")
};
```

Reading a highly confidential document outside the human's work assignments needs the data owner's
approval on record. This one is about the *human's scope of work*, and it applies whether the human
reads the document directly or an agent reads it for them. So it exists twice, once per principal
type, differing only in where the assignments come from:

```cedar
@id("forbid_offscope_sensitive_read_by_agent")
@checks("document.outside_operator_assignments")
@detail("The document's subject is not among the operator's assignments and no owner approval is recorded.")
forbid (
    principal is Acme::Agent,
    action == Acme::Action::"Read",
    resource is Acme::Document
)
when {
    resource.sensitivity == "highly_confidential" &&
    !principal.operator_assignments.contains(resource.subject)
}
unless {
    context.attestations.contains("data_owner_approved")
};
```

```cedar
@id("forbid_offscope_sensitive_read_by_user")
@checks("document.outside_operator_assignments")
@detail("The document's subject is not among the user's assignments and no owner approval is recorded.")
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
    context.attestations.contains("data_owner_approved")
};
```

Evaluated against Dana's request, the agent principal trips both agent policies:

```text
DENY   reason: forbid_agent_read_sensitive_unconfirmed
               forbid_offscope_sensitive_read_by_agent
```

Had Dana opened the document directly, only the scope rule would have fired:

```text
DENY   reason: forbid_offscope_sensitive_read_by_user
```

And once both pieces of evidence are recorded in the context, the same policy set allows the
request:

```text
ALLOW  reason: permit_read_documents
```

### Notes on the Shape of This

Each condition is its own policy. Cedar rejects a duplicate annotation key on one policy, so
`@checks` appears once per policy. Independent risk conditions stay independent policies, they are
reviewed separately, and the ones that matched are the ones reported in `reason()`.

Each `forbid` names the evidence that clears it, in its `unless` clause. A `forbid` with no `unless`
is a hard block: no amount of evidence changes the answer. The `unless` clause is the policy's real
statement of what would make the request acceptable, and it is enforced by the engine on the second
call rather than trusted to the application.

The policy names evidence, not a procedure. `data_owner_approved` says what has to be true. Whether
the challenge service collects it with an in-app approval, a ticket, or a call is not the policy's
concern, and the reasoning engine may decide not to try at all.

One rule, two principal types, two policies. They share one `@checks` value, so the reasoning
engine sees the same condition whichever principal type fired it, while each keeps its own `@id`
for the decision log.

## Application Code

```rust
use std::collections::BTreeSet;

// Server-issued records only, already filtered to this action and resource and to
// records that have not expired. Never the raw client context.
let mut attestations: BTreeSet<String> = evidence.valid_for(&document, Action::Read);

// A set: two policies reporting the same condition are one condition.
let checks = |result: &AuthorizeResult| -> BTreeSet<String> {
    cedarling
        .annotation_values(result.response.diagnostics().reason(), "checks")
        .into_iter()
        .collect()
};

// Evidence collected earlier is already in this request, so a read that needs no
// challenge is allowed here and never reaches the reasoning engine.
let first = cedarling
    .authorize_unsigned(read_request(&agent, &document, &attestations))
    .await?;
if first.decision {
    return Ok(serve(first));
}

// No labels: no policy named missing evidence, so there is nothing to collect.
let found = checks(&first);
if found.is_empty() {
    return Err(deny(&found));
}

audit.record(&cedarling.annotations_by_policy(first.response.diagnostics().reason()));

// The reasoning engine decides what, if anything, to ask for. It sees the conditions
// the policies found plus signals from outside this request.
let plan = reasoning_engine
    .decide(&agent, &document, &found, session.signals())
    .await?;

let challenges = match plan {
    Plan::Challenge(challenges) => challenges,
    Plan::Refuse | Plan::Contain => return Err(deny(&found)),
};

for challenge in challenges {
    // Results are attested by the challenge service and stored server-side,
    // bound to this resource and action, with the expiry the service set.
    let attestation = challenge_service.run(challenge, &agent, &document).await?;
    attestations.insert(evidence.record(attestation, &document, Action::Read)?);
}

// One more evaluation with the new evidence, and its answer is final.
let second = cedarling
    .authorize_unsigned(read_request(&agent, &document, &attestations))
    .await?;
if second.decision {
    Ok(serve(second))
} else {
    Err(deny(&checks(&second)))
}
```

There are three ways out before any challenge runs. The request is allowed on the first call,
because the evidence it needs was collected earlier or it never needed any. No policy labeled the
denial, so there is nothing to ask for. Or the reasoning engine refuses. The reasoning engine cannot
turn a `Deny` into an `Allow`; it can only collect evidence that the policies then evaluate.

Nothing in this code trusts the caller. The set fed into `context.attestations` is rebuilt from
server-side records that are scoped to this resource and action and dropped once they expire, which
is what makes the policy's `unless` clause mean anything.

There is exactly one evaluation after the evidence is collected. A second denial goes back to the
caller, not to the reasoning engine, because asking again until the answer is `Allow` turns a policy
mistake into an endless series of prompts.

## Requirements for the Evidence

The pattern is only as strong as the integrity of `context.attestations`, so treat these as part of
the implementation rather than as advice. They bind whoever runs the challenges: the reasoning
engine, the challenge service, and the application that assembles the context.

- Never accept evidence from the client. An attestation has to be produced by the challenge service
  and held in server-side session state, or in a signed, audience-bound token. If a browser or an
  agent can put `"human_intent_confirmed"` into the context, the policy enforces nothing.
- Expire it. Cedar has no concept of elapsed time here, so the challenge service sets a lifetime on
  every attestation and the application drops expired ones before building the context. An
  attestation with no expiry is one that a long-lived session rides forever.
- Bind it to the request. Evidence collected for one document should not silently authorize a bulk
  export of a thousand others. Scope the stored attestation to the action and resource it was
  collected for, or to a short window, whichever your risk model justifies.
- Collect it from someone who can refuse. An attestation an agent produces on its own is not
  evidence, and neither is one the requester produces about their own request.
  `human_intent_confirmed` has to come from the operator named by `operator_id`, never from the
  agent; `data_owner_approved` has to come from the team in `resource.owner_team`, never from the
  operator, or the operator approves their own off-scope read. Record who answered, and reject an
  answer from the wrong party.
- Log the failures. A challenge that is repeatedly abandoned is a signal in itself, and it belongs in
  the same stream as the decision logs, where the reasoning engine can see it next time.

Because those records live on the server, a browser evaluating these same policies is working from a
copy it cannot refresh, so its denials are provisional. See
[Advisory denials in a client-side PDP](./annotation-client-authority.md).

## Variation: Risk Carried by the Operator

Risk attaches to the human, but the request arrives from the agent. The copied
`operator_risk_tier` keeps the two connected, so an elevated-risk operator does not get a quieter
path by sending an assistant:

```cedar
@id("forbid_agent_read_for_elevated_risk_operator")
@checks("operator.elevated_risk")
@detail("The agent's operator is flagged elevated-risk and no security key verification is recorded.")
forbid (
    principal is Acme::Agent,
    action == Acme::Action::"Read",
    resource is Acme::Document
)
when {
    principal.operator_risk_tier == "elevated" &&
    resource.sensitivity == "highly_confidential"
}
unless {
    context.attestations.contains("security_key_verified")
};
```

## Variation: Containment

Some conditions should not be resolvable in the moment. After several failed challenges, or when an
investigation has already flagged the account, the right answer is a durable block that a person has
to lift.

That is the same mechanism with the `unless` clause removed:

```cedar
@id("forbid_contained_operator")
@checks("operator.contained")
@detail("The agent's operator is under containment by security operations.")
forbid (
    principal is Acme::Agent,
    action,
    resource
)
when { principal.operator_risk_tier == "contained" };
```

The human's own sessions need the twin, or containment stops an assistant and lets the operator
carry on by hand:

```cedar
@id("forbid_contained_user")
@checks("operator.contained")
@detail("The user is under containment by security operations.")
forbid (
    principal is Acme::User,
    action,
    resource
)
when { principal.risk_tier == "contained" };
```

No evidence clears these policies, so the reasoning engine has nothing to collect. It reads
`operator.contained` and knows not to offer a challenge; lifting the block means changing the risk
tier, which happens outside the request.

Containment depends on an attribute being present, and it is the one policy here that must never
stop applying unnoticed. Keep `risk_tier` and `operator_risk_tier` required in the schema. Cedarling
validates the principal against the schema when it builds it, so a principal without the attribute
fails the request instead of slipping past the containment policy. Without a schema there is nothing
to check: the policy errors at evaluation, drops out of the decision, and the denial it should have
produced never happens.

Containment here is keyed on the human. Freezing one misbehaving agent while its operator keeps
working would need a risk attribute of the agent's own and a third policy.

## What This Buys You

The policies state what each risky condition is and what evidence would make it acceptable, and the
engine enforces that on every call. The decision about how to collect the evidence stays where the
signals are: with the reasoning engine and the challenge service. Security engineers add a
condition, retire one, or tighten what counts as sufficient evidence by editing policies, and each
change is a diff you can review, test against recorded requests, and find later in the decision log.
