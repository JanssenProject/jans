---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Pattern: Quota and Threshold Warnings

!!! note "Illustrative example"

    The tiers, thresholds, and `@checks` values are invented for this page. Cedarling attaches no
    meaning to them; they work only because the example application reads them. Pick your own
    identifiers and your own thresholds. See [Policy Annotation Patterns](./annotation-patterns.md)
    for the ground rules.

## The Scenario

Acme's reporting API is sold in tiers. The `team` tier includes 1,000 generated reports per month,
`business` includes 25,000, and `enterprise` negotiates its own number in a contract. When the quota
runs out, report generation stops.

The denial itself is easy to get right. What is hard is everything before it: a customer whose first
signal is a hard failure in the middle of their month-end close is a customer with a support ticket
and a bad opinion of the product. They should have been told at 80%, told more loudly at 90%, and
offered a way forward before anything broke.

The authorization call already happens on every report generation, it already has the usage figure
in context, and the policy that allows the request already knows how close to the limit it is. The
missing piece is a way for the policy to say "allowed, and usage is past 80%", which is what a label
on a `permit` does. What to show the customer, and whether to offer an upgrade or call their account
manager, stays with the application.

## Labels on Permit Policies

Labels usually turn up on denials, but a `permit` carries them just as well, and Cedar reports them
the same way: on an `Allow`, `reason()` contains the satisfied `permit` policies, so the application
can read their annotations after a *successful* authorization.

That covers a category of "allowed, and here is something worth knowing":

- usage approaching a quota or rate limit
- an entitlement that expires soon
- access granted under a temporary exception rather than a standing rule

The quota case is the clearest of them, so it is the one worked through here.

## Where the Quota Number Lives

Cedar cannot count, so the usage figure has to arrive with the request: the application reads it
from its metering store, a counter, or a billing service, and passes it in as context. A client-side
PDP holding a stale copy of that counter is the subject of
[Advisory denials in a client-side PDP](./annotation-client-authority.md). The *limit* is a
different question, and it has two reasonable homes.

Put the limit in the policy when it defines a plan. The team tier including 1,000 reports is a
product decision that applies to every team customer, changes rarely, and should be reviewable as a
diff.

Put the limit on the entity when it is negotiated per customer. An enterprise contract for 40,000
reports is data about one account, and encoding it in a policy would mean a policy release for every
signed contract.

This page uses both, because a real product usually has both.

Wherever they come from, the application resolves them. `context.reports_used`, `resource.tier`, and
`resource.contract_quota` are the only things the enforcing policies look at, so a service that
forwards them from the caller has handed the caller its own quota: `reports_used: 0` on every
request, or `tier: "enterprise"` with a generous `contract_quota`. Read the counter from metering and
the workspace from your own store, on the server, on every call.

## Schema

```cedarschema
namespace Acme {
    entity User;

    entity Agent = {
        "operator_id": String,
    };

    entity Workspace = {
        "tier": String,
        "contract_quota"?: Long,
        "inference_budget": Long,
    };

    action "GenerateReport" appliesTo {
        principal: [User],
        resource: [Workspace],
        context: {
            "reports_used": Long,
        }
    };

    action "RunInference" appliesTo {
        principal: [Agent],
        resource: [Workspace],
        context: {
            "budget_used": Long,
        }
    };
}
```

`contract_quota` is optional. Self-serve workspaces do not carry it, because their limit comes from
the policies below.

## Three Jobs, Three Kinds of Policy

Splitting the policy set by *job* keeps each policy readable:

- one broad `permit` that lets the action happen at all
- one `forbid` per plan that stops it when the quota is gone
- threshold policies that exist only to carry labels

```cedar
@id("permit_generate_report")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
);
```

That grant says nothing about quotas, and it does not need to. Enforcement is the `forbid`'s job,
and in Cedar a `forbid` always wins.

## Plan Limits Written as Policy

The team plan's limit is a literal, because 1,000 reports is what the plan includes:

```cedar
@id("forbid_generate_report_team_quota_exhausted")
@checks("quota.reports.exhausted")
@detail("The workspace has used every report its plan includes this month.")
forbid (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "team" &&
    context.reports_used >= 1000
};
```

```cedar
@id("forbid_generate_report_business_quota_exhausted")
@checks("quota.reports.exhausted")
@detail("The workspace has used every report its plan includes this month.")
forbid (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "business" &&
    context.reports_used >= 25000
};
```

Both plans report the same condition, so they share one `@checks` value and the application handles
them identically. The `@id` still says which plan's rule fired.

Changing what a plan includes is now a policy change. Raising the team plan to 1,500 reports is one
number in one file, reviewed like any other change, and visible in the policy store's history.

## Threshold Policies

The warnings are not about access at all. Both bands are already allowed by the broad `permit`, so
these policies change no decision. They exist so that their labels appear in `reason()` when usage
falls in their band:

```cedar
@id("permit_generate_report_team_quota_80")
@checks("quota.reports.above_80_percent")
@detail("Usage is between 80% and 90% of the plan's monthly reports.")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "team" &&
    context.reports_used >= 800 &&
    context.reports_used < 900
};
```

```cedar
@id("permit_generate_report_team_quota_90")
@checks("quota.reports.above_90_percent")
@detail("Usage is above 90% of the plan's monthly reports.")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "team" &&
    context.reports_used >= 900 &&
    context.reports_used < 1000
};
```

The business plan gets the same two policies with its own numbers and the same labels:

```cedar
@id("permit_generate_report_business_quota_80")
@checks("quota.reports.above_80_percent")
@detail("Usage is between 80% and 90% of the plan's monthly reports.")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "business" &&
    context.reports_used >= 20000 &&
    context.reports_used < 22500
};

@id("permit_generate_report_business_quota_90")
@checks("quota.reports.above_90_percent")
@detail("Usage is above 90% of the plan's monthly reports.")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "business" &&
    context.reports_used >= 22500 &&
    context.reports_used < 25000
};
```

At 850 reports on a team workspace, two policies are satisfied and both appear in the decision:

```text
ALLOW  reason: permit_generate_report
               permit_generate_report_team_quota_80
```

Three properties of this shape constrain how you extend it.

The bands within a tier must not overlap. If two bands could hold at once, both would appear in
`reason()` and the application would get two labels for one request, leaving it to guess which one
is current.

A threshold policy is still a real `permit`. It changes nothing only because the broad grant
already allows the action. If someone later narrows that grant, these policies become the thing
that authorizes those requests. Keep their conditions no wider than the grant they shadow.

The cost is duplication: tiers multiplied by bands. That is tolerable for a handful of plans and
unpleasant for twenty. When the count gets uncomfortable, move the limit onto the entity, as the
enterprise tier does next.

## Negotiated Limits Read from the Entity

An enterprise workspace carries its own number, so one set of policies covers every contract.
Percentages have to be computed against it, and Cedar has no division operator, so cross-multiply:

```text
used / limit >= 80 / 100      becomes      used * 100 >= limit * 80
```

```cedar
@id("forbid_generate_report_enterprise_quota_exhausted")
@checks("quota.reports.exhausted")
@detail("The workspace has used every report its contract includes.")
forbid (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "enterprise" &&
    resource has contract_quota &&
    context.reports_used >= resource.contract_quota
};
```

```cedar
@id("permit_generate_report_enterprise_quota_80")
@checks("quota.reports.above_80_percent")
@detail("Usage is between 80% and 90% of the contracted reports.")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "enterprise" &&
    resource has contract_quota &&
    context.reports_used * 100 >= resource.contract_quota * 80 &&
    context.reports_used * 100 < resource.contract_quota * 90
};
```

```cedar
@id("permit_generate_report_enterprise_quota_90")
@checks("quota.reports.above_90_percent")
@detail("Usage is above 90% of the contracted reports.")
permit (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    resource.tier == "enterprise" &&
    resource has contract_quota &&
    context.reports_used * 100 >= resource.contract_quota * 90 &&
    context.reports_used < resource.contract_quota
};
```

Running out means something different for an enterprise customer. A self-serve customer can fix it
with a credit card; an enterprise customer has a contract and an account manager, and showing them
an upgrade button is the wrong answer. That difference is real, and it does not belong in the label.
The policy reports the same condition for every tier. The application already knows the tier, so its
own table decides between an upgrade offer and a call to the account manager.

Cedar's `Long` arithmetic errors on overflow, and a policy that errors is dropped from the
evaluation, so keep contract quotas to sane values. A number near the `Long` boundary takes exactly
the cross-multiplied policies out of the decision while the rest of the set carries on:

```text
reports_used = 9000000000000000000, contract_quota = i64::MAX
ALLOW
error while evaluating policy `permit_generate_report_enterprise_quota_80`: integer overflow
error while evaluating policy `permit_generate_report_enterprise_quota_90`: integer overflow
reason: permit_generate_report
```

The exhaustion `forbid` survives, because a plain `>=` cannot overflow, so enforcement holds and the
warnings are what disappear. The errors are in `diagnostics().errors()`, not in `reason()`, so an
enforcement point that only reads `reason()` will never notice.

## The Broad Grant Fails Open

Enforcement now lives entirely in the `forbid` policies, and every one of them is gated on a tier.
A workspace whose tier matches none of them is not denied. It is *allowed*, without limit, by the
broad `permit`. Close that gap explicitly:

```cedar
@id("forbid_generate_report_unconfigured_plan")
@checks("quota.reports.plan_unconfigured")
@detail("The workspace's tier has no quota rules, or an enterprise workspace has no contract_quota.")
forbid (
    principal,
    action == Acme::Action::"GenerateReport",
    resource
)
when {
    !(["team", "business"].contains(resource.tier)) &&
    !(resource.tier == "enterprise" && resource has contract_quota)
};
```

This is the policy that catches a new plan added to the price list before anyone wrote its quota
rules, and an enterprise workspace whose `contract_quota` was never set. Adding a tier means adding
its policies *and* widening this guard, which is a change a reviewer can see.

What is *not* in the labels matters as much: no counts, no reset date, no plan price. Those are
per-tenant values that change every day, and annotations are static strings shared by every request
that hits the policy. The application fills in "947 of 1,000, resets on 1 September".

## Application Code

The application owns one table from `@checks` values to reactions. Everything the old-style
annotations would have carried (message, severity, upgrade target, who to notify) lives here, keyed
by condition and refined by data the application already has:

```rust
let result = cedarling.authorize_unsigned(request).await?;
let diagnostics = result.response.diagnostics();

// A policy that errored is missing from reason(); make sure somebody sees it.
for error in diagnostics.errors() {
    log::warn!("policy evaluation error: {error}");
}

// Only one labeled policy can match at a time (see below), so the merged map is enough.
let annotations = cedarling.annotations_map(diagnostics.reason());
let checks = annotations.get("checks").map(String::as_str);

if !result.decision {
    return Err(match checks {
        Some("quota.reports.exhausted") => QuotaError::Exhausted {
            usage: metering.usage(&workspace)?,
            // Self-serve tiers can upgrade; contract tiers talk to their account manager.
            way_forward: offer_for(&workspace),
        },
        Some("quota.reports.plan_unconfigured") => {
            ops.page("billing-oncall", &workspace)?;
            QuotaError::Unavailable
        },
        _ => QuotaError::Denied,
    });
}

let mut response = generate_report()?;

// Allowed, but the policy may have found something worth telling the user.
response.notice = match checks {
    Some("quota.reports.above_80_percent") => Some(Notice::info("quota.reports.approaching")),
    Some("quota.reports.above_90_percent") => {
        Some(Notice::critical("quota.reports.nearly_exhausted").with_offer(offer_for(&workspace)))
    },
    _ => None,
};

Ok(response)
```

```javascript
const result = await cedarling.authorize_unsigned(JSON.stringify(request));
const { checks } = cedarling.annotations_map(result.response.diagnostics.reason);

// The reaction table lives in the application, keyed by condition.
const reactions = {
  "quota.reports.exhausted": { level: "blocked", messageId: "quota.reports.exhausted" },
  "quota.reports.above_80_percent": { level: "info", messageId: "quota.reports.approaching" },
  "quota.reports.above_90_percent": { level: "critical", messageId: "quota.reports.nearly_exhausted" },
};

// { used, limit, resetsOn } come from the metering response, not from the policy.
const reaction = reactions[checks];

if (!result.decision) {
  return blocked({
    text: t(reaction?.messageId ?? "access.denied.generic", { used, limit, resetsOn }),
    action: offerFor(workspace),
  });
}

if (reaction) {
  banner.show({
    level: reaction.level,
    text: t(reaction.messageId, { used, limit, resetsOn }),
    action: offerFor(workspace),
  });
}
```

`annotations_map` is safe here because only one labeled policy can match at a time. The broad grant
carries no `@checks`, the report bands are gated on a tier and do not overlap within it, and the
inference budget has a single band. The application code depends on that property, so state it in a
test. If you later add an overlapping policy, switch to `annotation_values` and decide explicitly
how to combine the labels.

## Variation: Agent Cost Budgets

An autonomous agent consumes resources far faster than a human would, and the budget is usually
gone by the time anyone notices. The same shape covers it:

```cedar
@id("permit_run_inference")
permit (
    principal,
    action == Acme::Action::"RunInference",
    resource
);
```

```cedar
@id("forbid_run_inference_budget_exhausted")
@checks("budget.inference.exhausted")
@detail("This request would take the workspace past its inference budget.")
forbid (
    principal,
    action == Acme::Action::"RunInference",
    resource
)
when { context.budget_used >= resource.inference_budget };
```

```cedar
@id("permit_run_inference_budget_80")
@checks("budget.inference.above_80_percent")
@detail("Inference spend is above 80% of the workspace budget.")
permit (
    principal,
    action == Acme::Action::"RunInference",
    resource
)
when {
    context.budget_used * 100 >= resource.inference_budget * 80 &&
    context.budget_used < resource.inference_budget
};
```

`context.budget_used` is projected usage: what the agent has already spent plus the cost of the
request being authorized, computed server-side. Pass pre-request usage instead and the `>=` check
lets through the request that carries the budget past its limit.

The principal is the agent itself, and the human accountable for the spend is the one who has to
hear about it. The application routes both labels to `principal.operator_id`, because it knows that
budget conditions on an agent are its operator's business. See
[Modeling the agent](./annotation-challenge.md#modeling-the-agent) for why the agent is a principal
in its own right and why the operator travels as an identifier rather than an entity reference.

## Testing

Threshold policies are cheap to test exhaustively, so do it. Authorize on both sides of every
boundary and assert the decision *and* the exact `@checks` value each time. For the team plan that
is 799, 800, 899, 900, 999, and 1,000.

Test every tier separately. The bands are written out per plan, so a typo in the business numbers
cannot be caught by a passing team test, and the gap it leaves is quiet: a usage figure that matches
no band is still allowed by the broad `permit`, so the request succeeds with no warning at all.

Assert the `forbid` boundaries hardest. They are the only policies enforcing anything, so a wrong
comparison there gives the plan away.

For the entity-driven tier, pick a contract quota that is not divisible by 10. At 333 the 80%
boundary is 267, not 266: cross-multiplication crosses as soon as `used * 100` reaches `quota * 80`,
while computing the threshold as `quota * 80 / 100` in integers would have put it a request earlier.

Test the unconfigured cases too: a tier nobody recognizes, and an enterprise workspace with no
`contract_quota`. Both should reach `forbid_generate_report_unconfigured_plan`. If either one comes
back as a plain `Allow`, that plan is running without a quota.

Finally, test the application's table against the policy store: every `@checks` value the store
uses should have an entry, and every entry should still be used. A label nobody maps is a warning
nobody sees.
