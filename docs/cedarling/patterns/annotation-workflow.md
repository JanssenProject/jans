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

## Part 1: A State Graph Made of Policies

Onboarding is a graph: each step unlocks the next, and the merchant cannot accept payments until
every step is done. Instead of encoding that order in the wizard, give each step its own `forbid`
policy that names itself.

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

Three annotations describe each missing step, and they only mean anything together, so read them
with `annotations_by_policy` and keep each policy's set intact:

```rust
let by_policy = cedarling.annotations_by_policy(result.response.diagnostics().reason());

let mut remaining: Vec<(u32, &str, &str)> = by_policy
    .values()
    .filter_map(|a| {
        Some((
            a.get("wizard_step")?.parse().ok()?,
            a.get("required_step")?.as_str(),
            a.get("redirect_route")?.as_str(),
        ))
    })
    .collect();

remaining.sort();
// [(2, "bank_account",   "onboarding.bank_account"),
//  (3, "identity_check", "onboarding.identity_check")]
```

The sort is not optional. `reason()` is a set of policy IDs with no defined iteration order, so the
policies come back in whatever order the set yields, and two separate `annotation_values` calls
cannot be zipped by position: the first value of one list does not necessarily belong to the same
policy as the first value of the other. Anything that has to stay paired belongs in
`annotations_by_policy`, and anything that has to stay ordered has to say so in an annotation and be
sorted by the application.

With that in hand, the application has a checklist rather than a single error. It can show the whole
remaining path instead of revealing one obstacle at a time, redirect to the lowest-numbered step,
and render the rest as upcoming.

`annotation_values` is still the right call when the values are independent and order does not
matter, such as collecting every `@challenge` a decision asked for.

Two properties make this worth doing:

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

The policy decides who approves. That is the part worth moving out of the code: thresholds and
approver groups change with company policy, audit findings, and regulation, while the mechanics of
raising an approval request do not.

```rust
let reason: Vec<_> = result.response.diagnostics().reason().collect();
let annotations = cedarling.annotations_map(reason.iter().copied());

if let Some(group) = annotations.get("approver_group") {
    approvals.request(
        group,
        annotations.get("sla_hours").and_then(|h| h.parse::<u32>().ok()),
        &withdrawal,
    )?;
}
```

Once the approval is recorded, the request is re-evaluated with `approvals` in the context, and the
same policy set allows it. As with challenges, the approval record has to be produced server-side.
A client that can put `"finance_lead"` into `context.approvals` has approved its own withdrawal.

Several independent conditions can escalate to different people. A withdrawal that is both large
and going to a newly added bank account trips two policies, and `annotation_values` returns both
approver groups, so the application requests both approvals rather than picking one.

## Part 3: Break-Glass Access

Support staff sometimes need to open a merchant's records during an incident. Denying that outright
means incidents take longer. Allowing it silently means an unmonitored path into customer data.

The middle option is to allow it under conditions the application has to enforce afterwards:

```cedar
@id("permit_support_break_glass")
@break_glass("true")
@requires_justification("true")
@ttl_seconds("900")
@audit("dual_control")
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
hints: the application has to collect a justification, expire the grant after 15 minutes, write a
dual-control audit record, and notify the merchant. An `Allow` whose obligations are ignored is a
worse outcome than a `Deny`, because it looks accountable and is not.

Keep break-glass a separate, narrow policy rather than a relaxation of the normal one. It is the
policy an auditor will ask about first, and it is far easier to answer when it stands alone with a
single `@id`, its own conditions, and its own annotations.

## Reading the Annotations Generically

Once several patterns share a vocabulary, the application can handle them in one place rather than
one branch per key:

```javascript
const reason = result.response.diagnostics.reason;
const annotations = cedarling.annotations_map(reason);

if (!result.decision) {
  const steps = cedarling.annotation_values(reason, "required_step");
  const routes = cedarling.annotation_values(reason, "redirect_route");

  if (steps.length > 0) {
    return showChecklist(steps, routes, annotations.user_message_id);
  }
  if (annotations.approver_group) {
    return requestApproval(annotations.approver_group, annotations.sla_hours);
  }
  return denyPlain(annotations.user_message_id ?? "access.denied.generic");
}
```

The order of these branches is a product decision: a request can be missing an onboarding step
*and* need an approval, and the application decides which obstacle to show first. Policies do not
rank themselves.

## What This Buys You

The onboarding order, the approval threshold, the approver group, and the break-glass conditions
all live in the policy store, in policies that say what they require. The wizard renders a
checklist it did not author, and adding a fourth onboarding step or lowering the approval threshold
to five thousand is a policy diff that ships without touching the application.
