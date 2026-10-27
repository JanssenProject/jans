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

    Acme Payments and every `@checks` value below are invented for this page. Cedarling attaches
    no meaning to them; they work only because the example application reads them. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme Payments registers new merchants in stages. A new merchant can log in immediately, but before
they can accept a card payment they have to submit business details, connect a bank account, and
pass an identity check. Later, once they are trading, a large withdrawal needs approval from a
second person, and support staff occasionally need to open a merchant's records during an incident.

Each of these is the same shape: the policy knows exactly which condition holds the request back,
or on which basis it was let through. Written the usual way, that knowledge is duplicated: the
onboarding wizard re-implements the prerequisite checks to know what to show, and the withdrawal
handler re-implements the approval threshold to know when to ask. Over time both copies stop
matching the policies that actually decide.

Labels remove the duplicate. Each policy reports the condition it found, and the application maps
that condition to a screen, an approver, or an audit procedure. The rules live in the policies; the
reactions live in the application; nothing is written twice.

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
done. Give each one its own `forbid` policy that reports which prerequisite is missing:

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
@checks("onboarding.business_details_missing")
@detail("The merchant has not submitted business details.")
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
@checks("onboarding.bank_account_missing")
@detail("The merchant has not connected a bank account.")
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
@checks("onboarding.identity_check_missing")
@detail("The merchant has not passed the identity check.")
forbid (
    principal,
    action == Acme::Action::"AcceptPayments",
    resource
)
unless {
    principal.completed_steps.contains("identity_check")
};
```

A merchant who has submitted business details but nothing else gets both remaining conditions back
in one call:

```text
DENY   reason: forbid_accept_payments_without_bank_account
               forbid_accept_payments_without_identity_check
```

Nothing in the policies orders the prerequisites, and nothing should: they are independent, and a
merchant can clear them in any order. The order in which they are *presented*, the route to each
form, and the copy on each screen are interface decisions, so they live in the application's step
list, keyed by the condition each policy reports:

```rust
/// The wizard, in display order. Each step names the condition that keeps it open.
const ONBOARDING_STEPS: &[Step] = &[
    Step { checks: "onboarding.business_details_missing", route: "onboarding.business_details" },
    Step { checks: "onboarding.bank_account_missing", route: "onboarding.bank_account" },
    Step { checks: "onboarding.identity_check_missing", route: "onboarding.identity_check" },
];

let found: BTreeSet<String> = cedarling
    .annotation_values(result.response.diagnostics().reason(), "checks")
    .into_iter()
    .collect();

let remaining: Vec<&Step> = ONBOARDING_STEPS
    .iter()
    .filter(|step| found.contains(step.checks))
    .collect();

// A denying condition the wizard cannot show is a bug to surface, not one to skip:
// otherwise the merchant completes every step on screen and is refused again.
let unknown: Vec<&String> = found
    .iter()
    .filter(|c| {
        c.starts_with("onboarding.") && !ONBOARDING_STEPS.iter().any(|s| s.checks == c.as_str())
    })
    .collect();
if !unknown.is_empty() {
    return Err(Error::UnmappedCheck(unknown.into_iter().cloned().collect()));
}
```

The application now has a checklist rather than a single error. It can show the whole remaining
path, send the merchant to the first open step, and render the rest as upcoming.

The step list is the one place where the application repeats something from the policy store: the
set of condition identifiers. Adding a fourth prerequisite therefore means a new policy *and* a new
entry in the list. The `unknown` check above is what catches a mismatch. A policy that is added
first fails clearly in testing instead of leaving merchants stuck in front of a wizard that thinks
they are done.

The same policies answer for every entry point. An API call, the dashboard, and a mobile client all
get the same remaining conditions, because they come from the decision rather than from each
client's copy of the flow.

## Part 2: Escalation and Approval

A large withdrawal is not simply blocked. It needs someone else to agree.

Withdrawals need their own `permit`, or clearing the approval would leave the request matching
nothing at all and Cedar would deny it by default, with nothing to explain why:

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
@checks("withdrawal.large_without_finance_approval")
@detail("Withdrawals above $10,000 need a recorded finance-lead approval.")
forbid (
    principal,
    action == Acme::Action::"Withdraw",
    resource
)
when { context.amount_cents > 1000000 }
unless { context.approvals.contains("finance_lead") };
```

The policy states the rule: above the threshold, a finance-lead approval has to be on record. Who
the finance leads are this week, how long they have to answer, and how the request reaches them are
operational facts, and they change with staffing and process rather than with policy. The
application maps the condition to its approval workflow:

```rust
let found = cedarling.annotation_values(result.response.diagnostics().reason(), "checks");

// Every condition that asked for an approval, not just the first one.
for check in &found {
    match check.as_str() {
        "withdrawal.large_without_finance_approval" => {
            approvals.request(ApprovalKind::FinanceLead, &withdrawal)?;
        },
        "withdrawal.new_destination_unverified" => {
            approvals.request(ApprovalKind::Treasury, &withdrawal)?;
        },
        other => return Err(Error::UnmappedCheck(vec![other.to_string()])),
    }
}
```

Several independent conditions can hold at once. If a second `forbid` covered a newly added
destination account, a withdrawal that is both large and newly routed would match both policies, and
the loop above requests both approvals in one pass. Reading one arbitrary value with
`annotations_map` would request one, and the retry would be refused again by a condition nobody
mentioned: not an over-authorization, but a flow that gets stuck, and nobody notices until a user
runs into it.

Once the approval is recorded, the request is re-evaluated with `approvals` in the context, and the
same policy set allows it. The approval record has to be produced server-side. A client that can put
`"finance_lead"` into `context.approvals` has approved its own withdrawal.

A server-side record is necessary and not enough on its own. `context.approvals` is a set of role
names, so by itself it says that *somebody* with that role approved *something*. Three properties
have to come from the record behind it, and none of them are visible in the policy:

- who approved. The record has to name the approver, and the application has to refuse an approval
  whose approver is the principal making the request.
- what they approved. Tie the record to one withdrawal: its id, its amount, its destination. Store
  it against the session instead and the first approval covers every withdrawal after it.
- when. Expire it.

Cedar has no string concatenation, so the policy cannot check `"finance_lead:<withdrawal-id>"`
itself. The application resolves the approval records for *this* withdrawal and passes only the
roles whose stored approval matches its id, amount, and destination. The set then means "approved,
for this request", and `contains("finance_lead")` is a claim the policy can trust.

## Part 3: Break-Glass Access

Support staff sometimes need to open a merchant's records during an incident. Always denying it
means incidents take longer. Allowing it without a record means an unwatched path into customer
data.

The middle option is a narrow `permit` that labels the basis it grants on:

```cedar
@id("permit_support_break_glass")
@checks("support.break_glass_during_incident")
@detail("On-call support opening merchant records under an open incident.")
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

The label arrives on the success path. It tells the application that this `Allow` came through the
break-glass rule rather than a standing grant, and the application's own break-glass procedure
takes over: collect a justification, expire the grant after the period the security team set,
record a second reviewer, notify the merchant. Those duties belong to the application because the
application is the one carrying them out. An allow whose follow-up is skipped is worse than a deny,
because it leaves a trail that looks accountable while nothing was enforced, so make the procedure
fail closed: if it cannot run, do not serve the records.

The policy does not gate on all of that. `context.incident_id` is a precondition: no incident, no
permit. The justification is not. If it has to be a real gate, record it server-side, put
`justification_id` in the context, and add it to the `when` clause. Otherwise be clear that it is an
audit duty, not a check.

The time limit needs the same care. `permit_support_break_glass` carries no time term, so
re-authorizing the same request returns the same `Allow` for as long as the incident stays open. The
application has to drop the grant when its period ends, *and* the server has to re-resolve the
incident on every request and refuse one that has been closed. Neither covers the other.

`context.incident_id` has to be built server-side from a validated incident record, exactly like the
approvals above. The policy only checks that the value is non-empty, so if a client can put a string
there, every support account holds a permanent break-glass key. Resolve the incident before
authorizing, reject the request when it is unknown or already closed, and pass in what the server
resolved rather than what the caller sent.

Keep break-glass a separate, narrow policy rather than a relaxation of the normal one. It is the
policy an auditor will ask about first, and it is far easier to answer when it stands alone with a
single `@id` and its own conditions.

## Reading the Labels in One Place

Once several flows share the convention, the application can dispatch on the condition in one
place:

```javascript
const reason = result.response.diagnostics.reason;

if (!result.decision) {
  const found = new Set(cedarling.annotation_values(reason, "checks"));

  // The wizard owns the order; the policies only say which steps are open.
  const steps = ONBOARDING_STEPS.filter((step) => found.has(step.checks));
  if (steps.length > 0) {
    return showChecklist(steps);
  }

  const approvals = [...found].filter((c) => APPROVAL_KINDS[c]);
  if (approvals.length > 0) {
    return requestApprovals(approvals.map((c) => APPROVAL_KINDS[c]));
  }

  return denyPlain("access.denied.generic");
}
```

`annotation_values` is the right call here, because each policy carries a single label and the
labels are independent. Reach for `annotations_by_policy` when you need the `@detail` that belongs
to each label, for instance in a debugging view.

The order of these branches is a product decision. One request carries one action, so onboarding
steps and withdrawal approvals never arrive together, but two approval conditions for one withdrawal
can, and the application decides how to present them.

## What This Buys You

The prerequisite checks, the approval threshold, and the break-glass conditions are each written
once, in a policy that says what it checks. The wizard, the approval workflow, and the break-glass
procedure react to those conditions without re-implementing them, and lowering the approval
threshold to $5,000 is a one-line policy diff that ships without touching the application.
