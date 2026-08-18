---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Pattern: Multi-Step Flows and Escalation

!!! note "Illustrative example"

    Acme Payments and every annotation key below (`@required_step`, `@redirect_route`,
    `@approval_required`, and the rest) are invented for this page. Cedarling attaches no meaning
    to them; they work only because the example application reads them. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme Payments onboards merchants in stages. A new merchant can log in immediately, but before they
can accept a card payment they have to submit business details, connect a bank account, and pass an
identity check. Later, once they are trading, a large withdrawal needs a second pair of eyes, and
support staff occasionally need to open a merchant's records during an incident.

Each of these is the same shape: an action the user is not allowed to perform *yet*, where the
system knows exactly what would make it allowed. Written the usual way, that knowledge ends up
spread across the codebase. The onboarding wizard hardcodes the order of steps, the withdrawal
handler hardcodes the approval threshold, and the support tool hardcodes what counts as a valid
break-glass. All three then drift apart from the policies that supposedly govern them.

Annotations let each rule state its own remedy, so the application can render a next step without
knowing which rule produced it.

## Schema

```cedarschema
namespace Acme {
    entity User = {
        "completed_steps": Set<String>,
        "role": String,
    };

    entity Merchant;

    action "AcceptPayments" appliesTo {
        principal: [User],
        resource: [Merchant],
        context: {}
    };

    action "Withdraw" appliesTo {
        principal: [User],
        resource: [Merchant],
        context: {
            "amount_cents": Long,
            "approvals": Set<String>,
        }
    };

    action "ViewMerchantRecords" appliesTo {
        principal: [User],
        resource: [Merchant],
        context: {
            "incident_id": String,
        }
    };
}
```

## Part 1: Prerequisites as Policies

Onboarding has three prerequisites, and the merchant cannot accept payments until all of them are
done. Instead of encoding them in the wizard, give each one its own `forbid` policy that names
itself.

Nothing here orders the prerequisites: the three policies are independent, and a merchant can clear
them in any order. `@wizard_step` orders the *display*, so the interface can present a sequence over
a set. If a step genuinely has to come after another, that belongs in the policy condition, not in
the annotation.

```cedar
@id("permit_accept_payments")
permit (
    principal,
    action == Acme::Action::"AcceptPayments",
    resource
);
```

```cedar
@id("forbid_accept_payments_without_business_details")
@required_step("business_details")
@wizard_step("1")
@redirect_route("onboarding.business_details")
@user_message_id("onboarding.business_details.required")
forbid (
    principal,
    action == Acme::Action::"AcceptPayments",
    resource
)
unless {
    principal.completed_steps.contains("business_details")
};
```

```cedar
@id("forbid_accept_payments_without_bank_account")
@required_step("bank_account")
@wizard_step("2")
@redirect_route("onboarding.bank_account")
@user_message_id("onboarding.bank_account.required")
forbid (
    principal,
    action == Acme::Action::"AcceptPayments",
    resource
)
unless {
    principal.completed_steps.contains("bank_account")
};
```

```cedar
@id("forbid_accept_payments_without_identity_check")
@required_step("identity_check")
@wizard_step("3")
@redirect_route("onboarding.identity_check")
@user_message_id("onboarding.identity_check.required")
forbid (
    principal,
    action == Acme::Action::"AcceptPayments",
    resource
)
unless {
    principal.completed_steps.contains("identity_check")
};
```

A merchant who has submitted business details but nothing else gets both remaining steps back in
one call:

```text
DENY   reason: forbid_accept_payments_without_bank_account
               forbid_accept_payments_without_identity_check
```

Four annotations describe each missing step, and they only mean anything together, so read them
with `annotations_by_policy` and keep each policy's set intact:

```rust
let by_policy = cedarling.annotations_by_policy(result.response.diagnostics().reason());

let mut remaining: Vec<(u32, &str, &str)> = Vec::new();

for (policy_id, a) in &by_policy {
    match (
        a.get("wizard_step").and_then(|s| s.parse::<u32>().ok()),
        a.get("required_step"),
        a.get("redirect_route"),
    ) {
        (Some(step), Some(name), Some(route)) => {
            remaining.push((step, name.as_str(), route.as_str()))
        }
        // A blocking policy that cannot be rendered is a bug to surface, not one to skip.
        _ => return Err(Error::MalformedAnnotation(policy_id.clone())),
    }
}

remaining.sort();
// [(2, "bank_account",   "onboarding.bank_account"),
//  (3, "identity_check", "onboarding.identity_check")]
```

A `filter_map` with a chain of `?` reads better and discards every group it cannot parse. A new
`forbid` that carries `@required_step` but no `@redirect_route` falls out of the list, so the user
sees two remaining steps while three policies deny them, completes both, and is refused again with
no explanation. This pattern pays off because adding a policy is enough, and that only holds when a
policy the application cannot render fails loudly instead of disappearing.

The sort is required. `reason()` is a set of policy IDs with no defined iteration
order, so the policies come back in whatever order the set yields, and two separate
`annotation_values` calls cannot be zipped by position: the first value of one list does not
necessarily belong to the same policy as the first value of the other. Anything that has to stay
paired belongs in `annotations_by_policy`, and anything that has to stay ordered has to say so in an
annotation and be sorted by the application.

With that in hand, the application has a checklist rather than a single error. It can show the whole
remaining path instead of revealing one obstacle at a time, redirect to the lowest-numbered step,
and render the rest as upcoming.

`annotation_values` is still the right call when the values are independent and order does not
matter, such as collecting every `@challenge` a decision asked for.

This shape has two practical benefits:

- Adding a step means adding one policy, not editing a state machine. Nothing else has to know the
  new step exists.
- The same policies answer for every entry point. An API call, the dashboard, and a mobile client
  all get the same list of remaining steps, because the list comes from the decision rather than
  from each client's copy of the flow.

### Where the Redirect Should Point

`@redirect_route("onboarding.bank_account")` names a route, not a URL. A logical name survives a
frontend restructure and lets a mobile client resolve it to a screen instead of a path. It also
keeps the policy store free of deployment details such as locale prefixes and version segments.

## Part 2: Escalation and Approval

A large withdrawal is not blocked outright. It needs someone else to agree, which is a resolvable
denial in exactly the way a challenge is.

Withdrawals need their own `permit`, or clearing the approval would leave the request matching
nothing at all and Cedar would deny it by default, with no annotations to explain why:

```cedar
@id("permit_withdraw")
permit (
    principal,
    action == Acme::Action::"Withdraw",
    resource
);
```

```cedar
@id("forbid_large_withdrawal_without_approval")
@approval_required("finance_lead")
@approver_group("finance-leads")
@sla_hours("4")
@user_message_id("withdrawal.approval_required")
forbid (
    principal,
    action == Acme::Action::"Withdraw",
    resource
)
when { context.amount_cents > 1000000 }
unless { context.approvals.contains("finance_lead") };
```

Two keys do different jobs here. `@approver_group` names who to ask, `@approval_required` names the
token the policy tests for, so the application asks `finance-leads` and, when they answer, records
`finance_lead` in `context.approvals`. Both come from the policy, so the code holds no mapping
between the two. Thresholds and approver groups change with company policy, audit findings, and
regulation, while the mechanics of raising an approval request do not.

```rust
let reason: Vec<_> = result.response.diagnostics().reason().collect();
let by_policy = cedarling.annotations_by_policy(reason.iter().copied());

// Every policy that asked for an approval, each with the SLA that belongs to it.
for annotations in by_policy.values() {
    let Some(group) = annotations.get("approver_group") else {
        continue;
    };

    let sla_hours = match annotations.get("sla_hours") {
        Some(value) => Some(
            value
                .parse::<u32>()
                .map_err(|_| Error::MalformedAnnotation("sla_hours".to_string()))?,
        ),
        None => None,
    };

    approvals.request(group, sla_hours, &withdrawal)?;
}
```

Two details in that loop decide whether it works or only looks like it does. It reads
`annotations_by_policy`, so a group is never paired with an SLA from a different policy and no
approval is dropped when two policies ask. And a malformed `@sla_hours` is an error rather than
`None`: a typo would otherwise produce an approval request with no deadline, and nothing surfaces
that until somebody asks why the escalation never fired.

Once the approval is recorded, the request is re-evaluated with `approvals` in the context, and the
same policy set allows it. As with challenges, the approval record has to be produced server-side.
A client that can put `"finance_lead"` into `context.approvals` has approved its own withdrawal.

A server-side record is necessary and not enough on its own. `context.approvals` is a set of role
names, so by itself it says that *somebody* with that role approved *something*. Three properties
have to come from the record behind it, and none of them are visible in the policy:

- who approved. The record has to name the approver, and the application has to refuse an approval
  whose approver is the principal making the request. Without that, a finance lead approves their
  own withdrawals and the policy cannot tell the difference.
- what they approved. Tie the record to one withdrawal: its id, its amount, its destination. Store
  it against the session instead and the first approval covers every withdrawal after it.
- when. Expire it, for the same reason a challenge expires.

To have the policy enforce the binding rather than trust the application to, put the binding in the
context as a value the policy can compare. Cedar has no string concatenation, so the policy cannot
assemble `"finance_lead:<withdrawal-id>"` itself: the application resolves the approval record for
*this* withdrawal and passes what it found, for instance
`context.approvals` holding only the roles whose stored approval matches this withdrawal's id and
amount. The set then means "approved, for this request", and `contains("finance_lead")` is a claim
the policy can trust.

Several independent conditions can escalate to different people. If a second `forbid` covered a
newly added destination account, a withdrawal that is both large and newly routed would trip both
policies, and `annotations_by_policy` hands back both groups with their own SLAs, so the application
requests both approvals at once.

`annotations_map` does not let it: one `approver_group` survives, so the application asks for one
approval. Cedar still refuses the retry, because the second `forbid` is untouched, and the user
waits out an approval only to be denied again by an obstacle nobody mentioned. The failure is a
stuck flow rather than an over-authorization, and it is invisible until somebody sits through it.

## Part 3: Break-Glass Access

Support staff sometimes need to open a merchant's records during an incident. Denying that outright
means incidents take longer. Allowing it silently means an unmonitored path into customer data.

The middle option is to allow it under conditions the application has to enforce afterwards:

```cedar
@id("permit_support_break_glass")
@break_glass("true")
@requires_justification("true")
@grant_ttl_seconds("900")
@audit("second_reviewer")
@notify("merchant_owner")
@user_message_id("support.break_glass.notice")
permit (
    principal,
    action == Acme::Action::"ViewMerchantRecords",
    resource
)
when {
    principal.role == "support_oncall" &&
    context.incident_id != ""
};
```

This is an `Allow`, so the annotations arrive on the success path. They are obligations rather than
hints: the application has to collect a justification, expire the grant after 15 minutes, write an
audit record naming a second reviewer, and notify the merchant. An `Allow` whose obligations are
ignored is worse than a `Deny`, because it leaves a trail that looks accountable while nothing was
enforced.

The policy does not gate on all of that. `context.incident_id` is a precondition: no incident, no
permit. `@requires_justification` is not. It is collected after the decision, so a support engineer
with a valid incident reads the record whether or not anyone ever types a reason, and the only thing
that fails is a log line. If the justification has to be a real gate, feed it
back the way challenges are fed back: record it server-side, put `justification_id` in the context,
and add it to the `when` clause. Otherwise be clear with yourself that it is an audit obligation.

`@grant_ttl_seconds` deserves the same scrutiny. `permit_support_break_glass` carries no time term,
so re-authorizing the same request returns the same `Allow` forever, and the 900 seconds means
nothing until something acts on it. Either the application drops the grant when it expires and stops
serving the records, or the server re-resolves the incident and refuses one that has been closed.
Re-authorization alone changes nothing.

`context.incident_id` has to be built server-side from a validated incident record, exactly like the
approvals above. The policy only checks that the value is non-empty, so if a client can put a string
there, every support account holds a permanent break-glass key and the audit trail fills up with
incident IDs that never existed. Resolve the incident before authorizing, reject the request when it
is unknown or already closed, and pass in what the server resolved rather than what the caller
sent.

Keep break-glass a separate, narrow policy rather than a relaxation of the normal one. It is the
policy an auditor will ask about first, and it is far easier to answer when it stands alone with a
single `@id`, its own conditions, and its own annotations.

## Reading the Annotations Generically

Once several patterns share a vocabulary, the application can handle them in one place rather than
one branch per key:

```javascript
const reason = result.response.diagnostics.reason;

if (!result.decision) {
  const byPolicy = Object.values(cedarling.annotations_by_policy(reason));

  // Each entry keeps its own step, route, and message together.
  const stepPolicies = byPolicy.filter((a) => a.required_step || a.wizard_step);
  const broken = stepPolicies.filter(
    (a) => !a.required_step || !a.wizard_step || !a.redirect_route,
  );

  if (broken.length > 0) {
    throw new MalformedAnnotation(broken);
  }

  const steps = stepPolicies.sort(
    (a, b) => Number(a.wizard_step) - Number(b.wizard_step),
  );

  if (steps.length > 0) {
    return showChecklist(steps);
  }

  // Every policy that asked for an approval, not just the first one.
  const approvals = byPolicy.filter((a) => a.approver_group);

  if (approvals.length > 0) {
    return requestApprovals(approvals);
  }

  const messages = cedarling.annotation_values(reason, "user_message_id");
  return denyPlain(messages[0] ?? "access.denied.generic");
}
```

Everything here goes through `annotations_by_policy`, because both branches need the annotations of
one policy to stay together: a step belongs with its own route, and an approver group belongs with
its own SLA. `annotations_map` would keep one `approver_group` and drop the rest, which turns the
two-approval case from the previous section into a single request that silently authorizes less than
it should.

The last line is the one case where an arbitrary pick is acceptable: several policies may carry a
`user_message_id`, and any of them is a reasonable thing to show. If that is not true for your
messages, rank them explicitly rather than taking the first.

The order of these branches is a product decision. One request carries one action, so onboarding
steps and withdrawal approvals never arrive together. Two obstacles for one action do collide once a
second approval rule exists, and policies do not rank themselves, so the application decides what to
show first.

## What This Buys You

The onboarding order, the approval threshold, the approver group, and the break-glass conditions
all live in the policy store, in policies that say what they require. The wizard renders a
checklist it did not author, and adding a fourth onboarding step or lowering the approval threshold
to $5,000 is a policy diff that ships without touching the application.
