---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - annotations
---

# Pattern: Shaping the Response by the Grant That Matched

!!! note "Illustrative example"

    Acme Analytics and every `@checks` value below are invented for this page. Cedarling attaches
    no meaning to them; they work only because the example application reads them. See
    [Policy Annotation Patterns](./annotation-patterns.md) for the ground rules.

## The Scenario

Acme Analytics lets internal staff query customer datasets. Access is not really a yes or no
question. A support agent looking up one account should see the account, but not the customer's
national ID. An analyst studying churn needs a hundred thousand rows and no personal identifiers at
all. An on-call engineer during an incident needs the raw record, and the company needs a record of
the fact.

Answering with `Deny` in all the awkward cases pushes people toward CSV exports and shared logins.
Answering with an unrestricted `Allow` gives away more than the request needed. The response has to
be shaped: some columns masked, some number of rows, watermarked on the way out.

Cedar decides *whether*, and the label on the matching `permit` says *on which basis*. The
application's data-handling rules turn that basis into a shape and enforce it.

## Why the Shape Is Not in the Policy

It is tempting to write the shape straight into the grant, as `@mask("national_id")` and
`@row_limit("100")`. That makes the policy an instruction to the application, and an instruction
has two problems.

It is only as good as the application's obedience. An application that ignores `@mask` ships the
column in the clear, and the policy that looked like a control did nothing. Nothing in the policy
store can tell.

And the shape depends on things the policy does not know: which columns exist in this version of
the dataset, how the response builder masks them, what the export format can carry. Column lists in
policies drift from schemas in databases.

So each grant states its basis with a label, and the application owns one table from basis to
handling. The masking rules are then written once, next to the code that implements them, and
reviewed with it.

## Schema

```cedarschema
namespace Acme {
    entity Analyst = {
        "clearance": String,
    };

    entity Dataset = {
        "classification": String,
        "special_category"?: {
            "regulation": String,
            "min_clearance": String,
        },
    };

    action "Query" appliesTo {
        principal: [Analyst],
        resource: [Dataset],
        context: {
            "purpose": String,
        }
    };
}
```

`special_category` is optional, and the policies test it with `resource has special_category`. A
dataset that holds nothing regulated simply does not carry the attribute, which reads better than a
`Bool` flag and gives the extra fields somewhere to live.

Both clearance fields are strings, and both get a guard policy below for values nobody expected.
`min_clearance` would be a natural fit for an enumerated entity type, but an entity reference inside
a record attribute cannot be built from an `authorize_unsigned` request, so it stays a string and
the policies check it.

Every policy touching an optional attribute has to guard it with `has`. Run `cedarling-cli validate`
against the policy store in CI (see [Policy Store](../reference/cedarling-policy-store.md)); its
strict schema validation rejects a policy that reaches for `resource.special_category.regulation`
without the check. Cedarling does not
validate policies on each authorization: an unguarded policy errors at request time and drops out of
the decision, taking its label with it.

## Policies

Each grant reports the basis it grants on. The baseline for standard clearance:

```cedar
@id("permit_query_standard")
@checks("grant.standard_clearance")
@control("SOC2-CC6.1")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when { principal.clearance == "standard" };
```

Elevated clearance needs its own baseline, or an elevated analyst doing ordinary work matches no
permit and falls to the default deny:

```cedar
@id("permit_query_elevated")
@checks("grant.elevated_clearance")
@control("SOC2-CC6.1")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    principal.clearance == "elevated" &&
    context.purpose != "incident_response"
};
```

Incident response gets a separate grant, so that the application can tell it apart:

```cedar
@id("permit_query_incident_response")
@checks("grant.incident_response")
@control("SOC2-CC7.2")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    principal.clearance == "elevated" &&
    context.purpose == "incident_response"
};
```

Special-category data carries its own grant, and it applies to every recognized clearance:

```cedar
@id("permit_query_special_category")
@checks("grant.special_category_data")
@regulation("GDPR-Art9")
@control("SOC2-CC6.1")
permit (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    resource has special_category &&
    ["standard", "elevated"].contains(principal.clearance)
};
```

The clearance test in that policy is doing real work. Without it the permit fires for any principal
at all, so an unrecognized clearance would be allowed on the most sensitive datasets while being
denied on ordinary ones. A grant that turns on a property of the *resource* still has to say who it
is for.

Clearance below what the dataset demands is a denial rather than a narrower grant:

```cedar
@id("forbid_query_below_required_clearance")
@checks("dataset.clearance_insufficient")
@detail("The dataset requires elevated clearance and the analyst holds standard clearance.")
forbid (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    resource has special_category &&
    resource.special_category.min_clearance == "elevated" &&
    principal.clearance == "standard"
};
```

Values nobody expected get guards of their own, one per side, so a typo in either an identity claim
or a dataset label is refused with a reason instead of falling through:

```cedar
@id("forbid_query_unrecognized_clearance")
@checks("analyst.clearance_unrecognized")
@detail("The analyst's clearance is neither standard nor elevated.")
forbid (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    !(["standard", "elevated"].contains(principal.clearance))
};
```

```cedar
@id("forbid_query_unrecognized_dataset_requirement")
@checks("dataset.clearance_requirement_unrecognized")
@detail("The dataset's min_clearance is neither standard nor elevated.")
forbid (
    principal,
    action == Acme::Action::"Query",
    resource
)
when {
    resource has special_category &&
    !(["standard", "elevated"].contains(resource.special_category.min_clearance))
};
```

## The Handling Table

The application maps each basis to its data handling. This table is the whole of the masking and
limiting policy, kept next to the code that applies it:

```rust
/// How a response granted on each basis must be shaped. A basis missing from this
/// table is a basis the application cannot honour, and the query is refused.
fn handling(checks: &str) -> Option<Handling> {
    Some(match checks {
        "grant.standard_clearance" => Handling {
            mask: &["national_id", "date_of_birth"],
            row_limit: Some(100),
            watermark: true,
        },
        "grant.elevated_clearance" => Handling {
            mask: &["national_id"],
            row_limit: Some(10_000),
            watermark: false,
        },
        "grant.incident_response" => Handling {
            mask: &[],
            row_limit: Some(100_000),
            watermark: false,
        },
        "grant.special_category_data" => Handling {
            mask: &["diagnosis_code"],
            row_limit: None,
            watermark: false,
        },
        _ => return None,
    })
}
```

Incident response has follow-up duties as well, a full audit of the query text and a notice to the
data protection officer, and they hang off the same `grant.incident_response` value in the
application's audit code.

## Overlapping Grants Need a Restrictive Merge

A standard analyst querying a dataset that carries `special_category` with `min_clearance` of
`"standard"` satisfies two grants, so both appear in the decision:

```text
ALLOW  reason: permit_query_standard
               permit_query_special_category
```

Each basis describes a limit, and picking one of them means dropping the others. Merge restrictively
instead: union the masks, take the smallest row limit, and treat a watermark as sticky. Where no
matching basis says anything, fall back to the application's own floor rather than to no limit at
all.

```rust
// Reached on an Allow. A Deny never gets here; the denial path is its own branch.
let found = cedarling.annotation_values(result.response.diagnostics().reason(), "checks");

// Start from the floor, not from "nothing": an absent rule must never widen anything.
let mut masked: BTreeSet<&str> = BASELINE_MASKED_COLUMNS.iter().copied().collect();
let mut row_limit = DEFAULT_ROW_LIMIT;
let mut watermark = WATERMARK_BY_DEFAULT;

for basis in &found {
    // Fail closed: a grant the application cannot shape is a grant it cannot honour.
    let rule = handling(basis).ok_or_else(|| Error::UnmappedCheck(vec![basis.clone()]))?;

    masked.extend(rule.mask.iter().copied());
    if let Some(limit) = rule.row_limit {
        row_limit = row_limit.min(limit);
    }
    watermark |= rule.watermark;
}

let rows = run_query(&query, row_limit)?;
Ok(shape(rows, &masked, watermark))
```

`DEFAULT_ROW_LIMIT` is the ceiling, and `min` can only lower it. If it started at unbounded, any
request whose grants carry no row limit, such as one matching only `grant.special_category_data`,
would be served with no limit at all.

The incident-response grant shows why the direction of the merge matters. Its limit of 100,000 is
wider than the 10,000 an elevated analyst normally gets, and a minimum merge cannot widen anything:
if both grants matched, the analyst would be held at 10,000. That is why `permit_query_elevated`
carries `context.purpose != "incident_response"`. The exclusion is what makes the wide grant
reachable.

Widening access therefore comes from *not* matching the narrow grant, never from adding a broader
one alongside it. Whenever you write a permit that is meant to grant more, check what else the same
request matches, because under a restrictive merge the narrowest matched basis decides.

## Mapping Policies to Controls

`@control("SOC2-CC6.1")` and `@regulation("GDPR-Art9")` are a different kind of label. They describe
the policy for people and tooling that never make an authorization call:

- an auditor asking "show me the controls that enforce CC6.1" gets an answer from the policy store,
  by searching for `@control("SOC2-CC6.1")`, rather than from a spreadsheet somebody maintains by
  hand
- the decision log records which control was exercised, because the annotations resolve from the
  same `reason()` the log already carries

```rust
// Written to the audit trail alongside the decision.
let reason: Vec<_> = result.response.diagnostics().reason().collect();
audit.record(Access {
    decision: result.decision,
    controls: cedarling.annotation_values(reason.iter().copied(), "control"),
    regulations: cedarling.annotation_values(reason.iter().copied(), "regulation"),
    by_policy: cedarling.annotations_by_policy(reason.iter().copied()),
});
```

`annotations_by_policy` is the right call for the audit record. It keeps the attribution intact, so
the log says *which* policy claimed CC6.1 rather than merely that something did.

Keep the compliance vocabulary as disciplined as the rest. A control ID that no longer exists in the
control framework, attached to a policy nobody has reviewed, produces a confident, wrong answer at
audit time.

## What This Buys You

Who may query what is stated once, in policies that say on which basis they grant. How each basis
shapes the data is stated once, in a table next to the code that masks and limits. Neither copies
the other, and a grant the application does not know how to shape is refused rather than served raw.
