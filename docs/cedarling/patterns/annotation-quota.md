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

    The tiers, thresholds, and annotation keys (`@warn`, `@upsell`, `@quota_reset`, and the rest)
    are invented for this page. Cedarling attaches no meaning to them; they work only because the
    example application reads them. Pick your own keys and your own thresholds. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme's reporting API is sold in tiers. The `team` tier includes 1,000 generated reports per month,
`business` includes 25,000, and `enterprise` negotiates its own number in a contract. When the quota
runs out, report generation stops.

The denial itself is easy to get right. What is hard is everything before it: a customer whose first
signal is a hard failure in the middle of their month-end close is a customer with a support ticket
and a bad opinion of the product. They should have been told at 80%, told more loudly at 90%, and
offered an upgrade before anything broke.

That warning is not a separate feature. The authorization call already happens on every report
generation, it already has the usage figure in context, and the policy that allows the request
already knows how close to the limit it is. The missing piece is a way for the policy to say
"allowed, but tell them", which is what an annotation on a `permit` does.

## Annotations on Permit Policies

Annotations are usually associated with denials, but a `permit` carries them just as well, and they
are reported the same way: on an `Allow`, `reason()` contains the satisfied `permit` policies, so
the application can read their annotations after a *successful* authorization.

That covers a whole category of "allowed, with something to say":

- approaching a quota or rate limit
- an entitlement that expires soon
- access granted under a temporary exception rather than a standing rule
- a tier-limited response the customer could unlock by upgrading

The quota case is the clearest of them, so it is the one worked through here.

## Where the Quota Number Lives

Cedar cannot count, so the usage figure has to arrive with the request: the application reads it
from its metering store, a counter, or a billing service, and passes it in as context. The *limit*
is a different question, and it has two reasonable homes.

Put the limit in the policy when it defines a plan. The team tier including 1,000 reports is a
product decision that applies to every team customer, changes rarely, and should be reviewable as a
diff. Writing it as a literal means the policy store answers "what does the team plan include?"
without joining against anything.

Put the limit on the entity when it is negotiated per customer. An enterprise contract for 40,000
reports is data about one account, and encoding it in a policy would mean a policy release for every
signed contract.

This page uses both, because a real product usually has both.

## Schema

```cedarschema
namespace Acme {
    entity User;

    entity Agent = {
        "operated_by": User,
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
- warning policies that exist only to carry annotations

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
@user_message_id("quota.reports.exhausted.self_serve")
@upsell("plan_business")
@quota_reset("monthly")
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
@user_message_id("quota.reports.exhausted.self_serve")
@upsell("plan_enterprise")
@quota_reset("monthly")
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

Changing what a plan includes is now a policy change. Raising the team plan to 1,500 reports is one
number in one file, reviewed like any other change, applied without a deployment of the billing
code, and visible in the policy store's history. Nobody has to ask which service owns the number.

## Annotation-Only Policies

The warnings are not about access at all. Both bands are already allowed by the broad `permit`, so
these policies change no decision. They exist so that their annotations appear in `reason()` when
usage falls in their band:

```cedar
@id("permit_generate_report_team_quota_80")
@warn("quota_80")
@warn_level("info")
@user_message_id("quota.reports.approaching")
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
@warn("quota_90")
@warn_level("critical")
@user_message_id("quota.reports.nearly_exhausted")
@upsell("plan_business")
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

The business plan gets the same two policies with its own numbers and its own upsell target:

```cedar
@id("permit_generate_report_business_quota_80")
@warn("quota_80")
@warn_level("info")
@user_message_id("quota.reports.approaching")
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
@warn("quota_90")
@warn_level("critical")
@user_message_id("quota.reports.nearly_exhausted")
@upsell("plan_enterprise")
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

Those two IDs arrive in no particular order, since `reason()` is a set. It does not matter here: the
broad grant carries no annotations, the warning policy carries all of them, and the application
reads one merged map without caring which policy contributed what.

Three things are worth knowing about this shape.

The bands within a tier must not overlap. Annotation values are static strings and Cedar rejects two
`@warn` annotations on one policy, so each band is its own policy. If two bands could hold at once,
both would appear in `reason()` and `annotation_values(reason, "warn")` would return two warnings
for one request, leaving the application to guess which is real.

An annotation-only policy is still a real `permit`. It changes nothing only because the broad grant
already allows the action. If someone later narrows that grant, these policies quietly become the
thing that authorizes those requests. Keep their conditions no wider than the grant they shadow, and
if that coupling makes you uncomfortable, put the warning annotations on the broad `permit` for the
default case and accept fewer bands.

The cost is duplication: tiers multiplied by bands. Three tiers with two bands each is six policies
that differ only in numbers, and adding a third band means touching every tier. That is tolerable
for a handful of plans and unpleasant for twenty. When the count gets uncomfortable, move the limit
onto the entity, as the enterprise tier does next, and accept that the number then lives in your data
rather than in your policies.

## Negotiated Limits Read from the Entity

An enterprise workspace carries its own number, so one set of policies covers every contract.
Percentages have to be computed against it, and Cedar has no division operator, so cross-multiply:

```text
used / limit >= 80 / 100      becomes      used * 100 >= limit * 80
```

```cedar
@id("forbid_generate_report_enterprise_quota_exhausted")
@user_message_id("quota.reports.exhausted.enterprise")
@notify("account_manager")
@escalate_to("csm-oncall")
@quota_reset("contract_term")
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
@warn("quota_80")
@warn_level("info")
@user_message_id("quota.reports.approaching")
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
@warn("quota_90")
@warn_level("critical")
@user_message_id("quota.reports.nearly_exhausted")
@notify("account_manager")
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

Running out means something different here. A self-serve customer can fix it with a credit card, so
those policies carry `@upsell`. An enterprise customer has a contract and an account manager, and
showing them an upgrade button is the wrong answer, so these carry `@notify` and `@escalate_to`
instead. The difference lives in the annotations rather than in the application's `if` statements.

Cedar's `Long` arithmetic errors on overflow, and a policy that errors is skipped, so keep contract
quotas to sane values. Multiplying a report counter by 100 leaves an enormous amount of headroom,
but a number near the `Long` boundary would take the request out of the policy set entirely.

## The Broad Grant Fails Open

Enforcement now lives entirely in the `forbid` policies, and every one of them is gated on a tier.
A workspace whose tier matches none of them is not denied. It is *allowed*, without limit, by the
broad `permit`.

That is the trade for the simpler structure, and it is the opposite of what a quota-gated grant would
do. Both failure modes are bad, but they are bad in different ways: a set of narrow permits fails
closed and silently, and a broad permit fails open and silently. Neither is acceptable, so close the
gap explicitly.

```cedar
@id("forbid_generate_report_unconfigured_plan")
@user_message_id("quota.reports.plan_unconfigured")
@escalate_to("billing-oncall")
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
its policies *and* widening this guard, which is a change a reviewer can see. Without it, the same
mistake ships unlimited free usage and nothing in the logs looks unusual.

Note also what is *not* in the annotations: no counts, no reset date, no plan price. Those are
per-tenant values that change every day, and annotations are static strings shared by every request
that hits the policy. The policy says *which* message and *which* plan, and the application fills in
"947 of 1,000, resets on 1 September".

## Application Code

```rust
let result = cedarling.authorize_unsigned(request).await?;

// Read once, so the iterator goes straight in without collecting.
let annotations = cedarling.annotations_map(result.response.diagnostics().reason());

if !result.decision {
    return Err(quota_error(
        annotations.get("user_message_id"),
        annotations.get("upsell"),
        annotations.get("escalate_to"),
    ));
}

let mut response = generate_report()?;

// Allowed, but the policy may still have something to say.
if let Some(warn) = annotations.get("warn") {
    response.notice = Some(Notice {
        message_id: annotations.get("user_message_id").cloned(),
        level: annotations
            .get("warn_level")
            .cloned()
            .unwrap_or_else(|| "info".to_string()),
        upsell: annotations.get("upsell").cloned(),
    });
    metrics.increment("quota_warning", warn);
}

Ok(response)
```

```javascript
const result = await cedarling.authorize_unsigned(JSON.stringify(request));
const annotations = cedarling.annotations_map(result.response.diagnostics.reason);

if (result.decision && annotations.warn) {
  banner.show({
    level: annotations.warn_level ?? "info",
    text: t(annotations.user_message_id, { used, limit, resetsOn }),
    action: annotations.upsell ? upgradeLink(annotations.upsell) : null,
  });
}
```

`annotations_map` is safe here because only one annotated policy can match at a time: the broad grant
carries nothing, and the bands are gated on a tier and do not overlap within it. That property is
worth stating in a test, because it is what the application code quietly depends on. If you later add
an overlapping policy, such as a per-workspace warning alongside a per-user one, switch to
`annotation_values` or `annotations_by_policy` and decide explicitly how to merge them.

Using `user_message_id` instead of literal copy keeps the banner translatable and lets it
interpolate the live numbers the policy does not have. See
[Use message IDs, not message text](./annotation-patterns.md#use-message-ids-not-message-text).

## Variation: Agent Cost Budgets

The same shape covers an autonomous agent burning through resources far faster than a human would,
where the budget is gone by the time anyone notices.

```cedar
@id("permit_agent_inference_budget_warn")
@warn("budget_80")
@cost_center("eng-ops")
@notify("operator")
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

The principal is the agent itself, and `@notify("operator")` tells the application to route the
warning to `principal.operated_by`, the human accountable for the spend and the only one who can do
anything about it. See
[Modeling the agent](./annotation-challenge.md#modeling-the-agent) for why the agent is a principal
in its own right rather than a flag on the human's request.

## Testing

Threshold policies are cheap to test exhaustively, so do it. Authorize on both sides of every
boundary and assert the decision *and* the exact `warn` value each time. For the team plan that is
799, 800, 899, 900, 999, and 1,000.

Test every tier separately. The bands are written out per plan, so a typo in the business numbers
cannot be caught by a passing team test, and the gap it leaves is quiet: a usage figure that matches
no band is still allowed by the broad `permit`, so the request succeeds with no warning at all. That
is a missing banner rather than a broken page, which is exactly why nobody notices it in staging.

Assert the `forbid` boundaries hardest. They are the only policies enforcing anything, so a wrong
comparison there sells the plan for free.

For the entity-driven tier, test an awkward contract quota such as 250, where the boundaries land at
200 and 225. Cross-multiplied comparisons are exact, so there is no rounding to argue about, but an
odd number is what catches a threshold written with the operands the wrong way round.

Test the unconfigured cases too: a tier nobody recognizes, and an enterprise workspace with no
`contract_quota`. Both should reach `forbid_generate_report_unconfigured_plan`. If either one comes
back as a plain `Allow`, the guard has a hole and that plan is running without a quota.
